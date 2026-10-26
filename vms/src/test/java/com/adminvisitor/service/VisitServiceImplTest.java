package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.RegistrationRequest;
import com.adminvisitor.dto.responsedto.RegistrationResponse;
import com.adminvisitor.entity.*;
import com.adminvisitor.enums.*;
import com.adminvisitor.exception.*;
import com.adminvisitor.mapper.VisitMapper;
import com.adminvisitor.repository.*;
import com.adminvisitor.service.impl.VisitManagementServiceImpl;
import com.adminvisitor.service.impl.VisitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceImplTest {

    @Mock private VisitRepository visitRepository;
    @Mock private VisitorRepository visitorRepository;
    @Mock private EmailService emailService;
    @Mock private IdGeneratorService idGeneratorService;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private BlacklistService blacklistService;
    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentService documentService;
    @Mock private ProofSecurityService proofSecurityService;
    @Mock private VisitManagementServiceImpl visitManagementService;
    @Mock private VisitMapper visitMapper;

    @InjectMocks
    private VisitServiceImpl service;

    @TempDir
    Path tempDir;

    private RegistrationRequest request;
    private Visitor visitor;
    private Employee employee;
    private RegistrationResponse response;

    @BeforeEach
    void setUp() {
        request = createRequest(VisitorType.VISITOR);
        visitor = createVisitor();
        employee = createEmployee();
        response = mock(RegistrationResponse.class);
    }

    private RegistrationRequest createRequest(VisitorType type) {
        return new RegistrationRequest(
                RegistrationType.PRE_REGISTRATION,
                type,
                "Arun",
                "Kumar",
                "arun@example.com",
                "9876543210",
                "ABC Technologies",
                "Business meeting",
                "EMP-001",
                LocalDate.now().plusDays(2),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                "Test visit",
                Nationality.DOMESTIC,
                "123456789012",
                "ABCDE1234F",
                null
        );
    }

    private Visitor createVisitor() {
        Visitor result = new Visitor();
        result.setId("VTR-001");
        result.setFirstName("Arun");
        result.setLastName("Kumar");
        result.setEmail("arun@example.com");
        result.setMobileNumber("9876543210");
        result.setCompanyName("ABC Technologies");
        return result;
    }

    private Employee createEmployee() {
        Employee result = new Employee();
        result.setId("EMP-001");
        result.setStatus("ACTIVE");
        return result;
    }

    private void mockNewVisitorRegistration() {
        when(visitorRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        when(visitorRepository.findByMobileNumber(request.mobileNumber()))
                .thenReturn(Optional.empty());

        when(visitorRepository.save(any(Visitor.class)))
                .thenAnswer(invocation -> {
                    Visitor savedVisitor = invocation.getArgument(0);
                    savedVisitor.setId("VTR-001");
                    return savedVisitor;
                });

        when(documentRepository
                .existsByVisitor_IdAndNationalityIsNotNull(nullable(String.class)))
                .thenReturn(false);

        when(employeeRepository.findById("EMP-001"))
                .thenReturn(Optional.of(employee));

        when(blacklistService.isBlacklistedByProof(
                any(ProofType.class), anyString()))
                .thenReturn(false);

        when(idGeneratorService.generateId(anyString(), anyString()))
                .thenReturn("VTR-001", "VIS-001");
    }

    private void mockSuccessfulVisitRegistration() {
        when(visitRepository.findOverlappingVisits(
                anyString(), any(), any(), anyList()))
                .thenReturn(List.of());

        when(visitRepository.save(any(Visit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(visitMapper.toRegistrationResponse(
                any(Visit.class), any(Visitor.class), anyString()))
                .thenReturn(response);
    }

    @Test
    void register_shouldRegisterNewVisitorSuccessfully() {
        mockNewVisitorRegistration();
        mockSuccessfulVisitRegistration();

        RegistrationResponse result = service.register(request);

        assertSame(response, result);
        verify(visitorRepository).save(any(Visitor.class));
        verify(documentService).saveIdentityProofs(
                any(Visitor.class),
                eq(Nationality.DOMESTIC),
                eq("123456789012"),
                eq("ABCDE1234F"),
                isNull()
        );
        verify(visitRepository).save(any(Visit.class));
        verify(emailService).sendVisitConfirmationEmail(any(Visit.class));
    }

    @Test
    void register_shouldRejectBlacklistedAadhaar() {
        when(blacklistService.isBlacklistedByProof(
                ProofType.AADHAAR, request.aadharNumber()))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> service.register(request));

        verify(visitorRepository, never()).save(any());
        verify(visitRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectBlacklistedPan() {
        when(blacklistService.isBlacklistedByProof(
                eq(ProofType.AADHAAR), anyString()))
                .thenReturn(false);

        when(blacklistService.isBlacklistedByProof(
                eq(ProofType.PAN), eq("ABCDE1234F")))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> service.register(request));

        verify(visitorRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectInvalidVisitTiming() {
        request = new RegistrationRequest(
                request.registrationType(),
                request.visitorType(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.mobileNumber(),
                request.companyName(),
                request.purpose(),
                request.hostId(),
                request.visitDate(),
                LocalTime.of(11, 0),
                LocalTime.of(10, 0),
                request.remarks(),
                request.nationality(),
                request.aadharNumber(),
                request.panNumber(),
                request.passportNumber()
        );

        assertThrows(BusinessRuleException.class,
                () -> service.register(request));

        verify(visitorRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectExistingEmail() {
        when(visitorRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(visitor));

        when(visitorRepository.findByMobileNumber(request.mobileNumber()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceAlreadyExistsException.class,
                () -> service.register(request));

        verify(visitorRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectExistingMobileNumber() {
        when(visitorRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        when(visitorRepository.findByMobileNumber(request.mobileNumber()))
                .thenReturn(Optional.of(visitor));

        assertThrows(ResourceAlreadyExistsException.class,
                () -> service.register(request));

        verify(visitorRepository, never()).save(any());
    }

    @Test
    void register_shouldReuseExistingVisitor() {
        when(visitorRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(visitor));

        when(visitorRepository.findByMobileNumber(request.mobileNumber()))
                .thenReturn(Optional.of(visitor));

        when(documentRepository
                .existsByVisitor_IdAndNationalityIsNotNull("VTR-001"))
                .thenReturn(true);

        Document document = new Document();
        document.setNationality(Nationality.DOMESTIC);

        when(documentService.getLatestIdentityDocument("VTR-001"))
                .thenReturn(document);

        when(documentService.verifyIdentityProof(
                visitor, ProofType.AADHAAR, request.aadharNumber()))
                .thenReturn(true);

        when(documentService.verifyIdentityProof(
                visitor, ProofType.PAN, request.panNumber()))
                .thenReturn(true);

        when(employeeRepository.findById("EMP-001"))
                .thenReturn(Optional.of(employee));

        when(idGeneratorService.generateId(anyString(), anyString()))
                .thenReturn("VIS-001");

        when(visitRepository.findOverlappingVisits(
                anyString(), any(), any(), anyList()))
                .thenReturn(List.of());

        when(visitRepository.save(any(Visit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(visitMapper.toRegistrationResponse(
                any(Visit.class), any(Visitor.class), anyString()))
                .thenReturn(response);

        assertSame(response, service.register(request));

        verify(visitorRepository, never()).save(any());
        verify(documentService, never()).saveIdentityProofs(
                any(), any(), any(), any(), any());
    }

    @Test
    void register_shouldRejectExistingVisitorWithDifferentNationality() {
        when(visitorRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(visitor));

        when(visitorRepository.findByMobileNumber(request.mobileNumber()))
                .thenReturn(Optional.of(visitor));

        when(documentRepository
                .existsByVisitor_IdAndNationalityIsNotNull("VTR-001"))
                .thenReturn(true);

        Document document = new Document();
        document.setNationality(Nationality.INTERNATIONAL);

        when(documentService.getLatestIdentityDocument("VTR-001"))
                .thenReturn(document);

        assertThrows(BusinessRuleException.class,
                () -> service.register(request));

        verify(visitRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectExistingVisitorWhenProofDoesNotMatch() {
        when(visitorRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(visitor));

        when(visitorRepository.findByMobileNumber(request.mobileNumber()))
                .thenReturn(Optional.of(visitor));

        when(documentRepository
                .existsByVisitor_IdAndNationalityIsNotNull("VTR-001"))
                .thenReturn(true);

        Document document = new Document();
        document.setNationality(Nationality.DOMESTIC);

        when(documentService.getLatestIdentityDocument("VTR-001"))
                .thenReturn(document);

        when(documentService.verifyIdentityProof(
                visitor, ProofType.AADHAAR, request.aadharNumber()))
                .thenReturn(false);

        when(documentService.verifyIdentityProof(
                visitor, ProofType.PAN, request.panNumber()))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> service.register(request));

        verify(visitRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectMissingHost() {
        when(visitorRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        when(visitorRepository.findByMobileNumber(request.mobileNumber()))
                .thenReturn(Optional.empty());

        when(visitorRepository.save(any(Visitor.class)))
                .thenAnswer(invocation -> {
                    Visitor savedVisitor = invocation.getArgument(0);
                    savedVisitor.setId("VTR-001");
                    return savedVisitor;
                });

        when(documentRepository
                .existsByVisitor_IdAndNationalityIsNotNull(anyString()))
                .thenReturn(false);

        when(idGeneratorService.generateId(anyString(), anyString()))
                .thenReturn("VTR-001");

        when(employeeRepository.findById("EMP-001"))
                .thenReturn(Optional.empty());

        when(blacklistService.isBlacklistedByProof(
                any(ProofType.class), anyString()))
                .thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.register(request));

        verify(visitRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectInactiveHost() {
        employee.setStatus("INACTIVE");

        when(visitorRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        when(visitorRepository.findByMobileNumber(request.mobileNumber()))
                .thenReturn(Optional.empty());

        when(visitorRepository.save(any(Visitor.class)))
                .thenAnswer(invocation -> {
                    Visitor savedVisitor = invocation.getArgument(0);
                    savedVisitor.setId("VTR-001");
                    return savedVisitor;
                });

        when(documentRepository
                .existsByVisitor_IdAndNationalityIsNotNull(anyString()))
                .thenReturn(false);

        when(idGeneratorService.generateId(anyString(), anyString()))
                .thenReturn("VTR-001");

        when(employeeRepository.findById("EMP-001"))
                .thenReturn(Optional.of(employee));

        when(blacklistService.isBlacklistedByProof(
                any(ProofType.class), anyString()))
                .thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.register(request));

        verify(visitRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectOverlappingVisit() {
        mockNewVisitorRegistration();

        when(visitRepository.findOverlappingVisits(
                nullable(String.class), any(), any(), anyList()))
                .thenReturn(List.of(new Visit()));

        assertThrows(BusinessRuleException.class,
                () -> service.register(request));

        verify(visitRepository, never()).save(any(Visit.class));
        verify(emailService, never())
                .sendVisitConfirmationEmail(any());
    }

    @Test
    void register_shouldCreateVendorWhenVisitorTypeIsVendor() {
        request = createRequest(VisitorType.VENDOR);

        mockNewVisitorRegistration();
        mockSuccessfulVisitRegistration();

        when(idGeneratorService.generateId(anyString(), anyString()))
                .thenReturn("VTR-001", "VND-001", "VIS-001");

        when(vendorRepository.findByVisitorId("VTR-001"))
                .thenReturn(Optional.empty());

        assertSame(response, service.register(request));

        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void register_shouldNotCreateDuplicateVendor() {
        request = createRequest(VisitorType.VENDOR);

        mockNewVisitorRegistration();
        mockSuccessfulVisitRegistration();

        when(idGeneratorService.generateId(anyString(), anyString()))
                .thenReturn("VTR-001", "VND-001", "VIS-001");

        when(vendorRepository.findByVisitorId("VTR-001"))
                .thenReturn(Optional.of(new Vendor()));

        assertSame(response, service.register(request));

        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    @Test
    void getVisitPhotoPath_shouldReturnExistingReadablePath()
            throws Exception {
        Path photo = Files.createFile(tempDir.resolve("visitor.jpg"));

        DocumentMetadata document = new DocumentMetadata();
        document.setDocumentPath(photo.toString());

        when(documentService.getVisitPhoto("VIS-001"))
                .thenReturn(document);

        assertEquals(photo.toString(),
                service.getVisitPhotoPath("VIS-001"));
    }

    @Test
    void getVisitPhotoPath_shouldRejectMissingPhoto() {
        DocumentMetadata document = new DocumentMetadata();
        document.setDocumentPath(
                tempDir.resolve("missing.jpg").toString());

        when(documentService.getVisitPhoto("VIS-001"))
                .thenReturn(document);

        assertThrows(ResourceNotFoundException.class,
                () -> service.getVisitPhotoPath("VIS-001"));
    }
}