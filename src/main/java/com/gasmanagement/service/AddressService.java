package com.gasmanagement.service;

import com.gasmanagement.dto.request.AddressRequest;
import com.gasmanagement.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> getMyAddresses(String userId);
    AddressResponse getAddressById(String addressId, String userId);
    AddressResponse getDefaultAddress(String userId);
    AddressResponse createAddress(String userId, AddressRequest request);
    AddressResponse updateAddress(String addressId, String userId, AddressRequest request);
    void deleteAddress(String addressId, String userId);
    AddressResponse setDefaultAddress(String addressId, String userId);
}
