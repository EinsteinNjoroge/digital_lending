package com.digital.lending.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.digital.lending.customer.service.CustomerService;
import com.digital.lending.customer.exception.ApiErrorResponse;
import com.digital.lending.customer.dto.CustomerDto;
import com.digital.lending.customer.dto.CreateCustomerRequest;
import com.digital.lending.customer.dto.UpdateCustomerRequest;

import static com.digital.lending.customer.util.CustomerApiConstants.*;

@RestController
@RequestMapping(BASE_PATH)
@Tag(name = TAG_NAME, description = TAG_DESCRIPTION)
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @Operation(summary = SUMMARY_CUSTOMER_CREATION, description = DESCRIPTION_CUSTOMER_CREATION)
    @ApiResponse(responseCode = "201", description = RESPONSE_201_CREATED_DESCRIPTION, content = @Content(schema = @Schema(implementation = CustomerDto.class)))
    @ApiResponse(responseCode = "400", description = RESPONSE_400_BAD_REQUEST_DESCRIPTION, content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = RESPONSE_409_CONFLICT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = RESPONSE_422_UNPROCESSABLE_DESCRIPTION, content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @GetMapping(PATH_VARIABLE_ID)
    @Operation(summary = SUMMARY_GET_CUSTOMER_BY_ID)
    @ApiResponse(responseCode = "200", description = RESPONSE_200_FOUND_DESCRIPTION)
    @ApiResponse(responseCode = "404", description = RESPONSE_404_NOT_FOUND_DESCRIPTION)
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable String id) {
        return customerService.findCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = SUMMARY_GET_ALL_CUSTOMERS)
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.findAllCustomers());
    }

    @PutMapping(PATH_VARIABLE_ID)
    @Operation(summary = SUMMARY_UPDATE_CUSTOMER_CONTACT)
    @ApiResponse(responseCode = "200", description = RESPONSE_200_UPDATED_DESCRIPTION)
    public ResponseEntity<CustomerDto> updateCustomer(@PathVariable String id, @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @DeleteMapping(PATH_VARIABLE_ID)
    @Operation(summary = SUMMARY_SOFT_DELETE_CUSTOMER)
    @ApiResponse(responseCode = "204", description = RESPONSE_204_DELETED_DESCRIPTION)
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}