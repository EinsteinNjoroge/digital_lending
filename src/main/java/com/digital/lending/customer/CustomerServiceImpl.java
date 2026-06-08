package com.digital.lending.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;

    public CustomerServiceImpl(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new DuplicateIdentityException("email", request.email());
        }

        Customer customer = switch (request) {
            case CreateCustomerRequest.Individual ind -> {
                if (ind.identities() == null || ind.identities().isEmpty()) {
                    throw new IdentityRequiredException();
                }

                IndividualCustomer individual = new IndividualCustomer();
                individual.setFirstName(ind.firstName());
                individual.setLastName(ind.lastName());
                individual.setDateOfBirth(ind.dateOfBirth());

                List<IdentityDocument> docs = ind.identities().stream()
                    .map(i -> {
                        validateGlobalUniqueness(i.documentNumber());
                        return new IdentityDocument(i.documentType(), i.documentNumber());
                    }).toList();
                individual.setIdentities(docs);
                yield individual;
            }

            case CreateCustomerRequest.Corporate corp -> {
                if (corp.directorIdentities() == null || corp.directorIdentities().isEmpty()) {
                    throw new CustomerDomainException("DIRECTORS_IDENTITY_REQUIRED", "Corporate registrations require identity documents for all directors.") {};
                }

                CorporateCustomer corporate = new CorporateCustomer();
                corporate.setCompanyName(corp.companyName());
                corporate.setRegistrationNumber(corp.registrationNumber());
                corporate.setIncorporationDate(corp.incorporationDate());
                corporate.setAuthorizedSignatoryName(corp.authorizedSignatoryName());

                List<IdentityDocument> docs = corp.directorIdentities().stream()
                    .map(d -> {
                        validateGlobalUniqueness(d.documentNumber());
                        return new IdentityDocument(d.documentType(), d.documentNumber());
                    }).toList();
                corporate.setDirectorIdentities(docs);
                yield corporate;
            }

            case CreateCustomerRequest.Joint jointReq -> {
                if (jointReq.applicantIdentities() == null || jointReq.applicantIdentities().size() != jointReq.numberOfApplicants()) {
                    throw new CustomerDomainException("JOINT_IDENTITIES_MISMATCH", "The provided document list count must match the total number of applicants.") {};
                }

                JointCustomer joint = new JointCustomer();
                joint.setAccountName(jointReq.accountName());
                joint.setPrimaryContactName(jointReq.primaryContactName());
                joint.setNumberOfApplicants(jointReq.numberOfApplicants());

                List<IdentityDocument> docs = jointReq.applicantIdentities().stream()
                    .map(a -> {
                        validateGlobalUniqueness(a.documentNumber());
                        return new IdentityDocument(a.documentType(), a.documentNumber());
                    }).toList();
                joint.setApplicantIdentities(docs);
                yield joint;
            }

            default -> throw new IllegalArgumentException("Unknown customer payload type");
        };

        customer.setId(UUID.randomUUID().toString());
        customer.setEmail(request.email());
        customer.setPhoneCountryCode(request.phoneCountryCode());
        customer.setPhoneNationalNumber(request.phoneNationalNumber());
        customer.setResidenceCountry(request.residenceCountry().toUpperCase());
        customer.setStatus(CustomerStatus.ACTIVE);

        return mapToDto(repository.save(customer));
    }

    private void validateGlobalUniqueness(String docNumber) {
        if (repository.existsInIndividualIdentities(docNumber) ||
            repository.existsInCorporateIdentities(docNumber) ||
            repository.existsInJointIdentities(docNumber)) {
            throw new DuplicateIdentityException("identity_document", docNumber);
        }
    }

    @Override
    public Optional<CustomerDto> findCustomerById(String id) {
        return repository.findById(id).map(this::mapToDto);
    }

    @Override
    public List<CustomerDto> findAllCustomers() {
        return repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerDto updateCustomer(String id, UpdateCustomerRequest request) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));

        if (!customer.getEmail().equalsIgnoreCase(request.email()) && repository.existsByEmail(request.email())) {
            throw new DuplicateIdentityException("email", request.email());
        }

        customer.setEmail(request.email());
        customer.setPhoneCountryCode(request.phoneCountryCode());
        customer.setPhoneNationalNumber(request.phoneNationalNumber());
        customer.setResidenceCountry(request.residenceCountry().toUpperCase());

        return mapToDto(repository.save(customer));
    }

    @Override
    @Transactional
    public void deleteCustomer(String id) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));

        // Compliance: Soft delete only
        customer.setStatus(CustomerStatus.DELETED);
        repository.save(customer);
    }

    @Override
    public boolean isCustomerActive(String id) {
        return repository.findById(id)
                .map(customer -> customer.getStatus() == CustomerStatus.ACTIVE)
                .orElse(false);
    }

    private CustomerDto mapToDto(Customer customer) {
        List<IdentityDocument> sourceDocs = switch (customer) {
            case IndividualCustomer ind -> ind.getIdentities();
            case CorporateCustomer corp -> corp.getDirectorIdentities();
            case JointCustomer joint -> joint.getApplicantIdentities();
            default -> List.of();
        };

        List<IdentityDto> mappedIdentities = sourceDocs.stream()
                .map(d -> new IdentityDto(d.getDocumentType(), d.getDocumentNumber()))
                .toList();

        return new CustomerDto(
                customer.getId(),
                customer.getCustomerType().name(),
                customer.getEmail(),
                customer.getPhoneCountryCode(),
                customer.getPhoneNationalNumber(),
                customer.getResidenceCountry(),
                customer.getStatus().name(),
                customer.getDisplayName(),
                mappedIdentities,
                customer.getCreatedAt()
        );
    }
}