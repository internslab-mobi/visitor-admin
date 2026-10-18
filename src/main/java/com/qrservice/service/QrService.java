
package com.qrservice.service;

public interface QrService {

    String generateQrCode(String payload);
    String generateToken();
}
