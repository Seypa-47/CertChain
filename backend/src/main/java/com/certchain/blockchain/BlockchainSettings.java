package com.certchain.blockchain;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.web3j.crypto.WalletUtils;

@Component
@ConditionalOnProperty(prefix = "app.blockchain", name = "enabled", havingValue = "true")
public class BlockchainSettings {
    private final String network;
    private final long chainId;
    private final String rpcUrl;
    private final String contractAddress;
    private final String privateKey;
    private final int confirmations;
    private final long deploymentBlock;
    private final Duration receiptTimeout;

    public BlockchainSettings(
        @Value("${app.blockchain.network}") String network,
        @Value("${app.blockchain.chain-id}") long chainId,
        @Value("${app.blockchain.rpc-url}") String rpcUrl,
        @Value("${app.blockchain.contract-address}") String contractAddress,
        @Value("${app.blockchain.issuer-private-key}") String privateKey,
        @Value("${app.blockchain.confirmations}") int confirmations,
        @Value("${app.blockchain.deployment-block}") long deploymentBlock,
        @Value("${app.blockchain.receipt-timeout}") Duration receiptTimeout
    ) {
        if (network.isBlank() || chainId <= 0 || rpcUrl.isBlank() || contractAddress.isBlank()
            || privateKey.isBlank() || confirmations < 1 || deploymentBlock < 0
            || receiptTimeout.isNegative() || receiptTimeout.isZero()) {
            throw new IllegalArgumentException("Invalid enabled blockchain configuration");
        }
        if (!WalletUtils.isValidAddress(contractAddress)) {
            throw new IllegalArgumentException("Invalid blockchain contract address");
        }
        this.network = network;
        this.chainId = chainId;
        this.rpcUrl = rpcUrl;
        this.contractAddress = contractAddress;
        this.privateKey = privateKey;
        this.confirmations = confirmations;
        this.deploymentBlock = deploymentBlock;
        this.receiptTimeout = receiptTimeout;
    }

    public String network() { return network; }
    public long chainId() { return chainId; }
    public String rpcUrl() { return rpcUrl; }
    public String contractAddress() { return contractAddress; }
    public String privateKey() { return privateKey; }
    public int confirmations() { return confirmations; }
    public long deploymentBlock() { return deploymentBlock; }
    public Duration receiptTimeout() { return receiptTimeout; }
}
