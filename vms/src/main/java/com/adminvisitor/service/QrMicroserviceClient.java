
package com.adminvisitor.service;

public interface QrMicroserviceClient {

    String generateToken();

    String generateQrCode(String payload);

}