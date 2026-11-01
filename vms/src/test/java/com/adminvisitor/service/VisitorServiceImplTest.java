package com.adminvisitor.service;


import com.adminvisitor.dto.requestdto.UpdateVisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.exception.ResourceAlreadyExistsException;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.mapper.VisitorMapper;
import com.adminvisitor.repository.*;
import com.adminvisitor.service.impl.VisitManagementServiceImpl;
import com.adminvisitor.service.impl.VisitorServiceImpl;
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
public class VisitorServiceImplTest {

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private  DocumentMetadataRepository documentMetadataRepository;
    @Mock
    private  BlacklistRepository blacklistRepository;
    @Mock
    private  VisitorRepository visitorRepository;
    @Mock
    private VisitManagementServiceImpl visitManagementService;
   @Mock
    private  VisitorMapper visitorMapper;
   @InjectMocks
   private VisitorServiceImpl visitorService;

private Visitor visitor;
   @BeforeEach
    void setUp(){
       visitor  =new Visitor();
       visitor.setId("VTR-001");
       visitor.setFirstName("anisha");
       visitor.setLastName("z");
       visitor.setEmail("anisha@gmail.com");
       visitor.setMobileNumber("9876543210");
       visitor.setCompanyName("mobi");



   }

   @Test
    void getVisitor_shouldReturnVisitor_whenVisitorExists(){
       when(visitorRepository.findById("VTR-001")).thenReturn(Optional.of(visitor));

       when(visitRepository.findTopByVisitorIdOrderByCreatedAtDesc("VTR-001")).thenReturn(Optional.empty());

       VisitorResponse expected=mock(VisitorResponse.class);

       when(visitorMapper.toResponse(visitor,null)).
               thenReturn(expected);

       VisitorResponse result =
               visitorService.getVisitor("VTR-001");

       assertNotNull(result);
       assertEquals(expected, result);

       verify(visitorRepository).findById("VTR-001");
       verify(visitRepository)
               .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001");
   }

   @Test

   void getVisitor_shouldThrowException_whenVisitorDoesNotExist() {
       when(visitorRepository.findById("VTR-001"))
               .thenReturn(Optional.empty());
       assertThrows(ResourceNotFoundException.class,
               () -> visitorService.getVisitor("VTR-001")
       );

       verify(visitorRepository).findById("VTR-001");

       verifyNoInteractions(visitorMapper);
   }

    @Test
    void getAllVisitors_shouldReturnEmptyList_whenNoVisitorsExist() {

        when(visitorRepository.findAll())
                .thenReturn(List.of());

        List<VisitorResponse> result =
                visitorService.getAllVisitors();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(visitorRepository).findAll();

        verifyNoInteractions(visitRepository);
    }

    @Test
    void getAllVisitors_shouldReturnVisitors_whenVisitorsExist() {

        Visitor visitor = new Visitor();
        visitor.setId("VTR-001");

        when(visitorRepository.findAll())
                .thenReturn(List.of(visitor));

        VisitorResponse response = mock(VisitorResponse.class);

        when(visitorMapper.toResponse(visitor, null))
                .thenReturn(response);

        List<VisitorResponse> result =
                visitorService.getAllVisitors();

        assertNotNull(result);
        assertEquals(1, result.size());
//        assertEquals(response, result.get(0));

        verify(visitorRepository).findAll();
        verify(visitorMapper).toResponse(visitor, null);
    }

    @Test
    void updateVisitor_shouldUpdateVisitor_whenValidRequest() {
        UpdateVisitorRequest request=new UpdateVisitorRequest(
                "Anisha","Z","ani@gmail.com","9876543210",
                "mobi"
        );

        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.of(visitor));


        when(visitorRepository.findByEmail("ani@gmail.com"))
                .thenReturn(Optional.empty());

        when(visitorRepository.findByMobileNumber("9876543210"))
                .thenReturn(Optional.of(visitor));

        when(visitorRepository.save(any(Visitor.class)))
                .thenReturn(visitor);
        when(visitRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.empty());

        VisitorResponse expected = mock(VisitorResponse.class);

        when(visitorMapper.toResponse(visitor, null))
                .thenReturn(expected);

        VisitorResponse result =
                visitorService.updateVisitor("VTR-001", request);

        assertNotNull(result);
        assertEquals(expected, result);

        assertEquals("Anisha", visitor.getFirstName());
        assertEquals("Z", visitor.getLastName());
        assertEquals("ani@gmail.com", visitor.getEmail());

        verify(visitorRepository).save(visitor);
        verify(vendorRepository).findByVisitorId("VTR-001");


    }
    @Test
    void updateVisitor_shouldThrowException_whenEmailAlreadyExists() {

        Visitor anotherVisitor = new Visitor();
        anotherVisitor.setId("VTR-002");

        UpdateVisitorRequest request = new UpdateVisitorRequest(
                "Anisha","Z","ani@gmail.com","9876543210",
                "mobi"
        );

        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.of(visitor));

        when(visitorRepository.findByEmail("existing@gmail.com"))
                .thenReturn(Optional.of(anotherVisitor));

        assertThrows(
                ResourceAlreadyExistsException.class,
                () -> visitorService.updateVisitor("VTR-001", request)
        );

        verify(visitorRepository, never()).save(any());
    }

    @Test
    void findVisitorByContact_shouldReturnVisitor_whenContactMatches() {

        when(visitorRepository
                .findByEmailIgnoreCaseAndMobileNumber(
                        "anisha@gmail.com",
                        "9876543210"
                ))
                .thenReturn(Optional.of(visitor));

        when(visitRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.empty());

        VisitorResponse expected = mock(VisitorResponse.class);

        when(visitorMapper.toResponse(visitor, null))
                .thenReturn(expected);

        VisitorResponse result =
                visitorService.findVisitorByContact(
                        " anisha@gmail.com ",
                        " 9876543210 "
                );

        assertNotNull(result);
        assertEquals(expected, result);

        verify(visitorRepository)
                .findByEmailIgnoreCaseAndMobileNumber(
                        "anisha@gmail.com",
                        "9876543210"
                );
    }
}
