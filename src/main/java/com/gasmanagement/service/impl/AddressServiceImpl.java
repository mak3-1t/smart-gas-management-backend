package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.AddressRequest;
import com.gasmanagement.dto.response.AddressResponse;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Address;
import com.gasmanagement.repository.AddressRepository;
import com.gasmanagement.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    @Override
    public List<AddressResponse> getMyAddresses(String userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse getAddressById(String addressId, String userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ giao hàng"));
        return mapToResponse(address);
    }

    @Override
    public AddressResponse getDefaultAddress(String userId) {
        Address address = addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseGet(() -> {
                    List<Address> addresses = addressRepository.findByUserId(userId);
                    return addresses.isEmpty() ? null : addresses.get(0);
                });

        if (address == null) {
            throw new ResourceNotFoundException("Chưa có địa chỉ giao hàng nào");
        }
        return mapToResponse(address);
    }

    @Override
    public AddressResponse createAddress(String userId, AddressRequest request) {
        List<Address> existing = addressRepository.findByUserId(userId);
        boolean shouldBeDefault = request.isDefault() || existing.isEmpty();

        if (shouldBeDefault && !existing.isEmpty()) {
            existing.forEach(a -> {
                if (a.isDefault()) {
                    a.setDefault(false);
                    addressRepository.save(a);
                }
            });
        }

        Address address = Address.builder()
                .userId(userId)
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .addressLine(request.getAddressLine())
                .ward(request.getWard())
                .district(request.getDistrict())
                .city(request.getCity())
                .isDefault(shouldBeDefault)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Khách hàng {} đã tạo địa chỉ mới: id={}", userId, saved.getId());
        return mapToResponse(saved);
    }

    @Override
    public AddressResponse updateAddress(String addressId, String userId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ"));

        if (request.isDefault() && !address.isDefault()) {
            addressRepository.findByUserId(userId).forEach(a -> {
                if (a.isDefault()) {
                    a.setDefault(false);
                    addressRepository.save(a);
                }
            });
            address.setDefault(true);
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setAddressLine(request.getAddressLine());
        address.setWard(request.getWard());
        address.setDistrict(request.getDistrict());
        address.setCity(request.getCity());

        Address saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    @Override
    public void deleteAddress(String addressId, String userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ"));

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserId(userId);
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
            }
        }
        log.info("Khách hàng {} đã xóa địa chỉ: id={}", userId, addressId);
    }

    @Override
    public AddressResponse setDefaultAddress(String addressId, String userId) {
        Address target = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ"));

        addressRepository.findByUserId(userId).forEach(a -> {
            if (a.isDefault()) {
                a.setDefault(false);
                addressRepository.save(a);
            }
        });

        target.setDefault(true);
        Address saved = addressRepository.save(target);
        return mapToResponse(saved);
    }

    private AddressResponse mapToResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .recipientName(a.getRecipientName())
                .phone(a.getPhone())
                .addressLine(a.getAddressLine())
                .ward(a.getWard())
                .district(a.getDistrict())
                .city(a.getCity())
                .isDefault(a.isDefault())
                .build();
    }
}
