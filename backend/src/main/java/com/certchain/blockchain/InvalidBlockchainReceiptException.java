package com.certchain.blockchain;

public class InvalidBlockchainReceiptException extends RuntimeException {
    public InvalidBlockchainReceiptException(String reason) { super(reason); }
}
