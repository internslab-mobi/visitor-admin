
package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.VendorEditResponse;
import com.adminvisitor.dto.responsedto.VendorResponse;
import com.adminvisitor.dto.responsedto.VisitorEditResponse;
import com.adminvisitor.entity.Vendor;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.mapper.VendorMapper;
import com.adminvisitor.repository.VendorRepository;
import com.adminvisitor.service.impl.VendorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorServiceImplTest {

    @Mock
    private VendorMapper vendorMapper;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private VisitorService visitorService;

    @InjectMocks
    private VendorServiceImpl vendorService;

    private Vendor vendor;
    private Visitor visitor;

    @BeforeEach
    void setUp() {

        visitor = new Visitor();
        visitor.setId("VTR-001");
        visitor.setFirstName("Anisha");
        visitor.setLastName("Z");
        visitor.setEmail("anisha@gmail.com");

        vendor = new Vendor();
        vendor.setId("VND-001");
        vendor.setVisitor(visitor);
    }


    @Test
    void getAllVendors_shouldReturnVendors_whenVendorsExist() {

        when(vendorRepository.findAll())
                .thenReturn(List.of(vendor));

        VendorResponse expected = mock(VendorResponse.class);

        when(vendorMapper.toResponse(vendor))
                .thenReturn(expected);

        List<VendorResponse> result =
                vendorService.getAllVendors();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(expected, result.get(0));

        verify(vendorRepository).findAll();
        verify(vendorMapper).toResponse(vendor);
    }

    @Test
    void getAllVendors_shouldReturnEmptyList_whenNoVendorsExist() {

        when(vendorRepository.findAll())
                .thenReturn(List.of());

        List<VendorResponse> result =
                vendorService.getAllVendors();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(vendorRepository).findAll();
        verifyNoInteractions(vendorMapper);
    }


    @Test
    void getVendorById_shouldReturnVendor_whenVendorExists() {

        when(vendorRepository.findById("VND-001"))
                .thenReturn(Optional.of(vendor));

        VendorResponse expected = mock(VendorResponse.class);

        when(vendorMapper.toResponse(vendor))
                .thenReturn(expected);

        VendorResponse result =
                vendorService.getVendorById("VND-001");

        assertNotNull(result);
        assertSame(expected, result);

        verify(vendorRepository).findById("VND-001");
        verify(vendorMapper).toResponse(vendor);
    }

    @Test
    void getVendorById_shouldThrowException_whenVendorDoesNotExist() {

        when(vendorRepository.findById("VND-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorService.getVendorById("VND-001")
        );

        verify(vendorRepository).findById("VND-001");
        verifyNoInteractions(vendorMapper);
    }


    @Test
    void getVendorEditDetails_shouldReturnDetails_whenVendorExists() {

        when(vendorRepository.findById("VND-001"))
                .thenReturn(Optional.of(vendor));

        VisitorEditResponse visitorEdit =
                mock(VisitorEditResponse.class);

        when(visitorService.getVisitorEditDetails("VTR-001"))
                .thenReturn(visitorEdit);

        VendorEditResponse expected =
                mock(VendorEditResponse.class);

        when(vendorMapper.toEditResponse(vendor, visitorEdit))
                .thenReturn(expected);

        VendorEditResponse result =
                vendorService.getVendorEditDetails("VND-001");

        assertNotNull(result);
        assertSame(expected, result);

        verify(vendorRepository).findById("VND-001");

        verify(visitorService)
                .getVisitorEditDetails("VTR-001");

        verify(vendorMapper)
                .toEditResponse(vendor, visitorEdit);
    }

    @Test
    void getVendorEditDetails_shouldThrowException_whenVendorDoesNotExist() {

        when(vendorRepository.findById("VND-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vendorService.getVendorEditDetails("VND-001")
        );

        verify(vendorRepository).findById("VND-001");

        verifyNoInteractions(visitorService);
        verifyNoInteractions(vendorMapper);
    }
}
