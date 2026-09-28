package com.certchain.blockchain;

import com.certchain.blockchain.generated.CertificateRegistry;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

@Component
@ConditionalOnProperty(prefix = "app.blockchain", name = "enabled", havingValue = "true")
public class Web3jCertificateRegistryGateway implements CertificateRegistryGateway {
    private static final BigInteger ISSUE_GAS_LIMIT = BigInteger.valueOf(350_000);
    private static final BigInteger REVOKE_GAS_LIMIT = BigInteger.valueOf(200_000);
    private final BlockchainSettings settings;
    private final Web3j web3j;
    private final RawTransactionManager transactions;
    private final CertificateRegistry contract;
    private final ChainIdentity identity;

    public Web3jCertificateRegistryGateway(BlockchainSettings settings) {
        this.settings = settings;
        this.web3j = Web3j.build(new HttpService(settings.rpcUrl()));
        Credentials credentials;
        try {
            credentials = Credentials.create(settings.privateKey());
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("Invalid blockchain issuer private key");
        }
        this.transactions = new RawTransactionManager(web3j, credentials, settings.chainId());
        this.contract = CertificateRegistry.load(settings.contractAddress(), web3j,
            transactions, new DefaultGasProvider());
        this.identity = new ChainIdentity(settings.network(), settings.chainId(),
            settings.contractAddress(), credentials.getAddress());
        validateStartup();
    }

    private void validateStartup() {
        try {
            if (actualChainId() != settings.chainId()) throw new IllegalStateException("Configured blockchain chain ID does not match RPC");
            String code = web3j.ethGetCode(settings.contractAddress(), DefaultBlockParameterName.LATEST)
                .send().getCode();
            if (code == null || code.equals("0x") || code.equals("0x0")) {
                throw new IllegalStateException("No contract code at configured blockchain address");
            }
            byte[] issuerRole = Numeric.hexStringToByteArray(Hash.sha3String("ISSUER_ROLE"));
            if (!contract.hasRole(issuerRole, identity.issuerAddress()).send()) {
                throw new IllegalStateException("Configured blockchain signer lacks ISSUER_ROLE");
            }
        } catch (IOException | org.web3j.protocol.exceptions.TransactionException error) {
            throw new IllegalStateException("Blockchain startup validation failed", error);
        } catch (Exception error) {
            if (error instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Blockchain startup validation failed", error);
        }
    }

    @Override public ChainIdentity identity() { return identity; }

    @Override
    public Optional<OnChainCertificate> findCertificate(String certificateKey) {
        try {
            if (actualChainId() != identity.chainId()) {
                throw new BlockchainUnavailableException("Blockchain chain changed");
            }
            var record = contract.getCertificate(bytes32(certificateKey)).send();
            if (record.issuedAt.signum() == 0) return Optional.empty();
            return Optional.of(new OnChainCertificate(Numeric.toHexString(record.certificateHash),
                record.issuedAt.longValueExact(), record.expiresAt.longValueExact(),
                record.revoked, record.issuer));
        } catch (Exception error) {
            throw unavailable(error);
        }
    }

    @Override
    public String submitIssue(String certificateKey, String certificateHash, long expiresAt) {
        try {
            Function function = new Function(CertificateRegistry.FUNC_ISSUECERTIFICATE,
                List.of(new Bytes32(bytes32(certificateKey)), new Bytes32(bytes32(certificateHash)),
                    new Uint64(BigInteger.valueOf(expiresAt))), List.of());
            String data = FunctionEncoder.encode(function);
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            EthSendTransaction sent = transactions.sendTransaction(gasPrice, ISSUE_GAS_LIMIT,
                settings.contractAddress(), data, BigInteger.ZERO);
            if (sent.hasError() || sent.getTransactionHash() == null) {
                throw new BlockchainUnavailableException("Blockchain submission failed");
            }
            return sent.getTransactionHash();
        } catch (IOException error) {
            throw unavailable(error);
        }
    }

    @Override
    public String submitRevoke(String certificateKey) {
        try {
            Function function = new Function(CertificateRegistry.FUNC_REVOKECERTIFICATE,
                List.of(new Bytes32(bytes32(certificateKey))), List.of());
            EthSendTransaction sent = transactions.sendTransaction(web3j.ethGasPrice().send().getGasPrice(),
                REVOKE_GAS_LIMIT, settings.contractAddress(), FunctionEncoder.encode(function), BigInteger.ZERO);
            if (sent.hasError() || sent.getTransactionHash() == null) {
                throw new BlockchainUnavailableException("Blockchain revocation submission failed");
            }
            return sent.getTransactionHash();
        } catch (IOException error) {
            throw unavailable(error);
        }
    }

    @Override
    public Optional<RevokeReceipt> findRevokeReceipt(String transactionHash) {
        try {
            EthGetTransactionReceipt result = web3j.ethGetTransactionReceipt(transactionHash).send();
            if (result.hasError()) throw new BlockchainUnavailableException("Blockchain receipt query failed");
            if (result.getTransactionReceipt().isEmpty()) return Optional.empty();
            TransactionReceipt receipt = result.getTransactionReceipt().orElseThrow();
            var block = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(receipt.getBlockNumber()), false)
                .send().getBlock();
            if (block == null) throw new BlockchainUnavailableException("Blockchain block unavailable");
            long blockNumber = receipt.getBlockNumber().longValueExact();
            long confirmations = web3j.ethBlockNumber().send().getBlockNumber().longValueExact() - blockNumber + 1;
            String topic = EventEncoder.encode(CertificateRegistry.CERTIFICATEREVOKED_EVENT);
            List<RevokeEvent> events = new ArrayList<>();
            for (Log log : receipt.getLogs()) {
                if (log.getTopics().isEmpty() || !topic.equalsIgnoreCase(log.getTopics().getFirst())) continue;
                var event = CertificateRegistry.getCertificateRevokedEventFromLog(log);
                events.add(new RevokeEvent("CertificateRevoked", Numeric.toHexString(event.certificateKey),
                    event.revokedBy, event.revokedAt.longValueExact(), log.getAddress()));
            }
            return Optional.of(new RevokeReceipt(receipt.getTransactionHash(), receipt.isStatusOK(),
                actualChainId(), receipt.getTo(), blockNumber,
                Instant.ofEpochSecond(block.getTimestamp().longValueExact()), confirmations, events));
        } catch (IOException error) {
            throw unavailable(error);
        }
    }

    @Override
    public Optional<RevokeReceipt> findRevokeByKey(String certificateKey) {
        try {
            EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(BigInteger.valueOf(settings.deploymentBlock())),
                DefaultBlockParameterName.LATEST, settings.contractAddress());
            filter.addOptionalTopics(EventEncoder.encode(CertificateRegistry.CERTIFICATEREVOKED_EVENT));
            filter.addOptionalTopics(certificateKey);
            EthLog response = web3j.ethGetLogs(filter).send();
            if (response.hasError()) throw new BlockchainUnavailableException("Blockchain event query failed");
            List<EthLog.LogResult<?>> logs = response.getLogs();
            for (int index = logs.size() - 1; index >= 0; index--) {
                Object raw = logs.get(index).get();
                if (raw instanceof Log log && settings.contractAddress().equalsIgnoreCase(log.getAddress())) {
                    return findRevokeReceipt(log.getTransactionHash());
                }
            }
            return Optional.empty();
        } catch (IOException error) {
            throw unavailable(error);
        }
    }

    @Override
    public Optional<IssueReceipt> findReceipt(String transactionHash) {
        try {
            EthGetTransactionReceipt result = web3j.ethGetTransactionReceipt(transactionHash).send();
            if (result.hasError()) throw new BlockchainUnavailableException("Blockchain receipt query failed");
            if (result.getTransactionReceipt().isEmpty()) return Optional.empty();
            TransactionReceipt receipt = result.getTransactionReceipt().orElseThrow();
            long blockNumber = receipt.getBlockNumber().longValueExact();
            var block = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(receipt.getBlockNumber()), false)
                .send().getBlock();
            if (block == null) throw new BlockchainUnavailableException("Blockchain block unavailable");
            long confirmations = web3j.ethBlockNumber().send().getBlockNumber().longValueExact()
                - blockNumber + 1;
            List<IssueEvent> events = new ArrayList<>();
            String issueTopic = EventEncoder.encode(CertificateRegistry.CERTIFICATEISSUED_EVENT);
            for (Log log : receipt.getLogs()) {
                if (log.getTopics().isEmpty() || !issueTopic.equalsIgnoreCase(log.getTopics().getFirst())) continue;
                var event = CertificateRegistry.getCertificateIssuedEventFromLog(log);
                events.add(new IssueEvent("CertificateIssued", Numeric.toHexString(event.certificateKey),
                    Numeric.toHexString(event.certificateHash), event.issuer,
                    event.issuedAt.longValueExact(), event.expiresAt.longValueExact(), log.getAddress()));
            }
            return Optional.of(new IssueReceipt(receipt.getTransactionHash(), receipt.isStatusOK(),
                actualChainId(), receipt.getTo(), blockNumber, Instant.ofEpochSecond(block.getTimestamp().longValueExact()),
                confirmations, events));
        } catch (IOException error) {
            throw unavailable(error);
        }
    }

    @Override
    public Optional<IssueReceipt> findIssueByKey(String certificateKey) {
        try {
            EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(BigInteger.valueOf(settings.deploymentBlock())),
                DefaultBlockParameterName.LATEST, settings.contractAddress());
            filter.addOptionalTopics(EventEncoder.encode(CertificateRegistry.CERTIFICATEISSUED_EVENT));
            filter.addOptionalTopics(certificateKey);
            EthLog response = web3j.ethGetLogs(filter).send();
            if (response.hasError()) throw new BlockchainUnavailableException("Blockchain event query failed");
            List<EthLog.LogResult<?>> logs = response.getLogs();
            for (int index = logs.size() - 1; index >= 0; index--) {
                Object raw = logs.get(index).get();
                if (raw instanceof Log log && settings.contractAddress().equalsIgnoreCase(log.getAddress())) {
                    return findReceipt(log.getTransactionHash());
                }
            }
            return Optional.empty();
        } catch (IOException error) {
            throw unavailable(error);
        }
    }

    private long actualChainId() throws IOException {
        return web3j.ethChainId().send().getChainId().longValueExact();
    }

    private static byte[] bytes32(String value) {
        byte[] bytes = Numeric.hexStringToByteArray(value);
        if (bytes.length != 32) throw new IllegalArgumentException("Expected bytes32 hex value");
        return bytes;
    }

    private static BlockchainUnavailableException unavailable(Exception cause) {
        return new BlockchainUnavailableException("Blockchain RPC unavailable", cause);
    }

    @PreDestroy
    void close() { web3j.shutdown(); }
}
