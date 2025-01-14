/*---------------------------------------------------------------------
*	Copyright (c) 2021 Veeva Systems Inc.  All Rights Reserved.
*	This code is based on pre-existing content developed and
*	owned by Veeva Systems Inc. and may only be used in connection
*	with the deliverable with which it was provided to Customer.
*---------------------------------------------------------------------
*/
package com.veeva.vault.vapil.api.request;

import com.veeva.vault.vapil.api.client.VaultClient;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import com.veeva.vault.vapil.extension.VaultClientParameterResolver;
import com.veeva.vault.vapil.api.model.response.*;
import com.veeva.vault.vapil.api.model.response.AuditMetadataResponse.AuditMetadata;
import com.veeva.vault.vapil.api.model.response.AuditTypesResponse.AuditTrail;
import com.veeva.vault.vapil.api.model.response.DocumentAuditResponse.DocumentAudit;
import com.veeva.vault.vapil.api.model.response.DomainAuditResponse.DomainAuditData;
import com.veeva.vault.vapil.api.model.response.LoginAuditResponse.LoginAuditData;
import com.veeva.vault.vapil.api.model.response.ObjectAuditResponse.ObjectAuditData;
import com.veeva.vault.vapil.api.model.response.SystemAuditResponse.SystemAuditData;
import com.veeva.vault.vapil.api.model.response.EmailNotificationHistoryResponse.EmailNotification;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Tag("LogRequest")
@ExtendWith(VaultClientParameterResolver.class)
public class LogRequestTest {

	@Test
	public void testRetrieveAuditTypes(VaultClient vaultClient) {
		AuditTypesResponse response = vaultClient.newRequest(LogRequest.class)
				.retrieveAuditTypes();

		Assertions.assertTrue(response.isSuccessful());
		for (AuditTrail auditTrail : response.getAuditTrails()) {
			Assertions.assertNotNull(auditTrail.getName());
			System.out.println(auditTrail.getName());
		}
	}

	@Test
	public void testRetrieveAuditMetadata(VaultClient vaultClient) {
		AuditMetadataResponse response = vaultClient.newRequest(LogRequest.class)
				.retrieveAuditMetadata(LogRequest.AuditTrailType.DOCUMENT);

		Assertions.assertTrue(response.isSuccessful());
		AuditMetadata metadata = response.getData();
		Assertions.assertNotNull(metadata.getName());

		for (AuditMetadata.Field field : metadata.getFields()) {
			Assertions.assertNotNull(field.getName());
			Assertions.assertNotNull(field.getType());
		}
	}

	@Test
	@DisplayName("Should successfully retrieve document audit details with specified query params")
	public void testRetrieveDocumentAuditDetails(VaultClient vaultClient) {
		DocumentAuditResponse response = vaultClient.newRequest(LogRequest.class)
				.setStartDateTime(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(29))
				.setEndDateTime(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1))
				.setLimit(4)
				.setEvents(new HashSet<>(Arrays.asList("UploadDocBulk", "ExportBinder")))
				.retrieveAuditDetails(LogRequest.AuditTrailType.DOCUMENT);

		Assertions.assertTrue(response.isSuccessful());
		AuditDetailsResponse.ResponseDetails auditDetails = response.getResponseDetails();
		Assertions.assertNotNull(auditDetails.getDetailsObject().getName());
		Assertions.assertNotNull(auditDetails.getDetailsObject().getUrl());

		for (DocumentAudit documentAuditData : response.getData()) {
			Assertions.assertNotNull(documentAuditData.getId());
			Assertions.assertNotNull(documentAuditData.getTimestamp());
		}

	}

	@Test
	public void testRetrieveDomainAuditDetails(VaultClient vaultClient) {
		DomainAuditResponse response = vaultClient.newRequest(LogRequest.class)
				.setStartDateTime(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(10))
				.retrieveAuditDetails(LogRequest.AuditTrailType.DOMAIN);

		Assertions.assertTrue(response.isSuccessful());
		AuditDetailsResponse.ResponseDetails details = response.getResponseDetails();
		Assertions.assertNotNull(details.getDetailsObject().getName());
		Assertions.assertNotNull(details.getDetailsObject().getUrl());

		for (DomainAuditData data : response.getData()) {
			Assertions.assertNotNull(data.getId());
			Assertions.assertNotNull(data.getTimestamp());
		}
	}

	@Test
	public void testRetrieveLoginAuditDetails(VaultClient vaultClient) {
		LoginAuditResponse response = vaultClient.newRequest(LogRequest.class)
				.setStartDateTime(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(10))
				.retrieveAuditDetails(LogRequest.AuditTrailType.LOGIN);

		Assertions.assertTrue(response.isSuccessful());
		AuditDetailsResponse.ResponseDetails details = response.getResponseDetails();
		Assertions.assertNotNull(details.getDetailsObject().getName());
		Assertions.assertNotNull(details.getDetailsObject().getUrl());

		for (LoginAuditData data : response.getData()) {
			Assertions.assertNotNull(data.getId());
			Assertions.assertNotNull(data.getTimestamp());
			Assertions.assertNotNull(data.getUserName());
		}
	}

	@Test
	@DisplayName("Should successfully retrieve object audit details with specified query params")
	public void testRetrieveObjectAuditDetails(VaultClient vaultClient) {
		ObjectAuditResponse response = vaultClient.newRequest(LogRequest.class)
				.setStartDateTime(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(20))
				.setLimit(10)
				.setEvents(new HashSet<>(Arrays.asList("Create", "Update")))
				.retrieveAuditDetails(LogRequest.AuditTrailType.OBJECT);

		Assertions.assertTrue(response.isSuccessful());
		AuditDetailsResponse.ResponseDetails details = response.getResponseDetails();
		Assertions.assertNotNull(details.getDetailsObject().getName());
		Assertions.assertNotNull(details.getDetailsObject().getUrl());

		for (ObjectAuditData data : response.getData()) {
			Assertions.assertNotNull(data.getId());
			Assertions.assertNotNull(data.getTimestamp());
			Assertions.assertNotNull(data.getRecordId());
		}

		if (response.isPaginated()) {
			ObjectAuditResponse paginatedResponse = vaultClient.newRequest(LogRequest.class)
					.retrieveAuditDetailsByPage(LogRequest.AuditTrailType.OBJECT,
							response.getResponseDetails().getNextPage());
			Assertions.assertTrue(paginatedResponse.isSuccessful());
		}
	}

	@Test
	public void testRetrieveObjectAuditDetailsAsync(VaultClient vaultClient) {
		JobCreateResponse response = vaultClient.newRequest(LogRequest.class)
				.setAllDates(true)
				.setFormatResult(LogRequest.FormatResultType.CSV)
				.retrieveAuditDetails(LogRequest.AuditTrailType.OBJECT);

		Assertions.assertTrue(response.isSuccessful());
		Assertions.assertNotNull(response.getJobId());
	}

	@Test
	public void testRetrieveSystemAuditDetails(VaultClient vaultClient) {
		LogRequest request = vaultClient.newRequest(LogRequest.class);
		request.setStartDateTime(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(20));
		request.setEndDateTime(ZonedDateTime.now(ZoneId.of("UTC")));
		request.setAllDates(false);
		request.setFormatResult(LogRequest.FormatResultType.JSON);
		request.setLimit(4);

		SystemAuditResponse response = request.retrieveAuditDetails(LogRequest.AuditTrailType.SYSTEM);
		Assertions.assertTrue(response.isSuccessful());

		AuditDetailsResponse.ResponseDetails details = response.getResponseDetails();
		Assertions.assertNotNull(details.getDetailsObject().getName());
		Assertions.assertNotNull(details.getDetailsObject().getUrl());

		for (SystemAuditData data : response.getData()) {
			Assertions.assertNotNull(data.getId());
			Assertions.assertNotNull(data.getTimestamp());
			Assertions.assertNotNull(data.getAction());
		}

		// Test paging
		if (details.hasNextPage()) {
			response = request.retrieveAuditDetailsByPage(LogRequest.AuditTrailType.SYSTEM, details.getNextPage());
			details = response.getResponseDetails();
			Assertions.assertNotNull(details.getNextPage());
			Assertions.assertNotNull(details.getPreviousPage());
			Assertions.assertNotNull(details.getTotal());
		}

		if (details.hasPreviousPage()) {
			response = request.retrieveAuditDetailsByPage(LogRequest.AuditTrailType.SYSTEM, details.getPreviousPage());
			details = response.getResponseDetails();
			Assertions.assertNotNull(details.getNextPage());
			Assertions.assertNotNull(details.getTotal());
		}
	}

	@Test
	public void testRetrieveDomainFullAuditTrailAsCsv(VaultClient vaultClient) {
		JobCreateResponse response = vaultClient.newRequest(LogRequest.class)
				.setAllDates(true)
				.setFormatResult(LogRequest.FormatResultType.CSV)
				.retrieveAuditDetails(LogRequest.AuditTrailType.DOMAIN);

		Assertions.assertTrue(response.isSuccessful());
		Assertions.assertNotNull(response.getJobId());
		Assertions.assertNotNull(response.getUrl());
	}

	@Test
	public void testRetrieveSystemAuditDetailsAsCsv(VaultClient vaultClient) {

		JobCreateResponse response = vaultClient.newRequest(LogRequest.class)
				.setStartDateTime(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(29))
				.setFormatResult(LogRequest.FormatResultType.CSV)
				.retrieveAuditDetails(LogRequest.AuditTrailType.SYSTEM);

		Assertions.assertTrue(response.isSuccessful());
		Assertions.assertNotNull(response.getJobId());
		Assertions.assertNotNull(response.getUrl());
	}

	@Test
	public void testRetrieveSingleDocumentAuditDetails(VaultClient vaultClient) {
		String vql = String.format("select id from documents where version_modified_date__v > '%s'",
				ZonedDateTime.now(ZoneId.of("UTC")).minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));

		QueryResponse queryResponse = vaultClient.newRequest(QueryRequest.class).query(vql);
		Assertions.assertTrue(queryResponse.isSuccessful());

		Integer id = 1;
		for (QueryResponse.QueryResult rec : queryResponse.getData()) {
			id = rec.getInteger("id");
			break;
		}

		// Omit start and end dates to use the defaults (see the API guide)
		DocumentAuditResponse response = vaultClient.newRequest(LogRequest.class)
				.setLimit(4) // Just pull 4 records so the results can be viewed more easily
				.setFormatResult(LogRequest.FormatResultType.JSON)
				.retrieveDocumentAuditTrail(id);
		Assertions.assertTrue(response.isSuccessful());

		AuditDetailsResponse.ResponseDetails details = response.getResponseDetails();
		Assertions.assertNotNull(details.getDetailsObject().getName());
		Assertions.assertNotNull(details.getDetailsObject().getLabel());
		Assertions.assertNotNull(details.getDetailsObject().getUrl());

		for (DocumentAudit data : response.getData()) {
			Assertions.assertNotNull(data.getId());
			Assertions.assertNotNull(data.getAction());
			Assertions.assertNotNull(data.getDocumentUrl());
		}
	}

	@Test
	public void testRetrieveSingleDocumentAuditDetailsAsCsv(VaultClient vaultClient) {
		String vql = String.format("select id from documents where version_modified_date__v > '%s'",
				ZonedDateTime.now(ZoneId.of("UTC")).minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));

		QueryResponse queryResponse = vaultClient.newRequest(QueryRequest.class).query(vql);
		Assertions.assertTrue(queryResponse.isSuccessful());

		Integer id = 1;
		for (QueryResponse.QueryResult rec : queryResponse.getData()) {
			id = rec.getInteger("id");
			break;
		}

		DocumentAuditResponse response = vaultClient.newRequest(LogRequest.class)
				.setFormatResult(LogRequest.FormatResultType.CSV)
				.retrieveDocumentAuditTrail(id);
		Assertions.assertTrue(response.isSuccessful());
	}

	@Test
	public void testRetrieveSingleObjectAuditDetails(VaultClient vaultClient) {

		// Omit start and end dates to use the defaults (see the API guide)
		ObjectAuditResponse response = vaultClient.newRequest(LogRequest.class)
				.setFormatResult(LogRequest.FormatResultType.JSON)
				.retrieveObjectAuditTrail("user__sys", "2051303");

		Assertions.assertTrue(response.isSuccessful());
		if (response.isSuccessful()) {
			AuditDetailsResponse.ResponseDetails details = response.getResponseDetails();
			System.out.println("Offset = " + details.getOffset());
			System.out.println("Limit = " + details.getLimit());
			System.out.println("Size = " + details.getSize());
			System.out.println("Total = " + details.getTotal());
			System.out.println("Object/Name = " + details.getDetailsObject().getName());
			System.out.println("Object/Label = " + details.getDetailsObject().getLabel());
			System.out.println("Object/Url = " + details.getDetailsObject().getUrl());

			System.out.println("Items ****");
			for (ObjectAuditData data : response.getData()) {
				System.out.println("\n**** Data Item **** ");
				System.out.println("id = " + data.getId());
				System.out.println("timestamp = " + data.getTimestamp());
				System.out.println("UserName = " + data.getUserName());
				System.out.println("Full Name = " + data.getFullName());
				System.out.println("Action = " + data.getAction());
				System.out.println("Item = " + data.getItem());
				System.out.println("Record ID = " + data.getRecordId());
				System.out.println("Object Label = " + data.getObjectLabel());
				System.out.println("Workflow Name = " + data.getWorkflowName());
				System.out.println("Task Name = " + data.getTaskName());
				System.out.println("Verdict = " + data.getVerdict());
				System.out.println("Reason = " + data.getReason());
				System.out.println("Capacity = " + data.getCapacity());
				System.out.println("Event Description = " + data.getEventDescription());
				System.out.println("On Behalf Of = " + data.getOnBehalfOf());
			}
		}
	}

	@Test
	public void testRetrieveSingleObjectAuditDetailsAsCsv(VaultClient vaultClient) {
		System.out.println("\n****** Retrieve Single Object Audit Details As CSV ******");

		// Omit start and end dates to use the defaults (see the API guide)
		ObjectAuditResponse response = vaultClient.newRequest(LogRequest.class)
				.setFormatResult(LogRequest.FormatResultType.CSV)
				.retrieveObjectAuditTrail("product__v", "00P000000000601");

		if (response.isSuccessful()) {
			String results = new String(response.getBinaryContent());
			System.out.println(results);
		}

		System.out.println("Test complete...");
	}


	@Nested
	@DisplayName("Test Retrieve Email Notification History")
	class testRetrieveEmailNotificationHistory {
		@Test
		@DisplayName("with no query parameters")
		void noQueryParameters(VaultClient vaultClient) {
			EmailNotificationHistoryResponse response = vaultClient.newRequest(LogRequest.class)
					.retrieveEmailNotificationHistory();

			System.out.println("Response Status: " + response.getResponseStatus());
			System.out.println("Response Message: " + response.getResponse());
			assertTrue(response.isSuccessful());

			EmailNotificationHistoryResponse.ResponseDetails details = response.getResponseDetails();
			System.out.println("Response Details ****");
			System.out.println("Offset = " + details.getOffset());
			System.out.println("Limit = " + details.getLimit());
			System.out.println("Size = " + details.getSize());
			System.out.println("Total = " + details.getTotal());

			System.out.println("Items ****");
			for (EmailNotification data : response.getData()) {
				System.out.println("id = " + data.getNotificationId());
				System.out.println("Send Date = " + data.getSendDate());
				System.out.println("Recipient Email: " + data.getRecipientEmail());
				assertNotNull(data.getNotificationId());
				assertNotNull(data.getSendDate());
			}
		}

		@Test
		@DisplayName("with invalid query parameters")
		void invalidQueryParameters(VaultClient vaultClient) {
			ZonedDateTime startDate = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(29);

			EmailNotificationHistoryResponse response = vaultClient.newRequest(LogRequest.class)
					.setStartDateTime(startDate)
					.retrieveEmailNotificationHistory();

			System.out.println("Response Status: " + response.getResponseStatus());
			System.out.println("Response Message: " + response.getResponse());
			assertFalse(response.isSuccessful());

		}

		@Test
		@DisplayName("with start and end date/time query parameters")
		void startAndEndDateTimeQueryParameters(VaultClient vaultClient) {

			EmailNotificationHistoryResponse response = vaultClient.newRequest(LogRequest.class)
					.setStartDateTime(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(30))
					.setEndDateTime(ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1))
					.retrieveEmailNotificationHistory();

			System.out.println("Response Status: " + response.getResponseStatus());
			System.out.println("Response Message: " + response.getResponse());
			assertTrue(response.isSuccessful());

			EmailNotificationHistoryResponse.ResponseDetails details = response.getResponseDetails();
			System.out.println("Response Details ****");
			System.out.println("Offset = " + details.getOffset());
			System.out.println("Limit = " + details.getLimit());
			System.out.println("Size = " + details.getSize());
			System.out.println("Total = " + details.getTotal());

			System.out.println("Items ****");
			for (EmailNotification data : response.getData()) {
				System.out.println("id = " + data.getNotificationId());
				System.out.println("Send Date: " + data.getSendDate());
				System.out.println("Recipient Email: " + data.getRecipientEmail());
				assertNotNull(data.getNotificationId());
				assertNotNull(data.getSendDate());
			}
		}

		@Test
		@DisplayName("with start and end date query parameters")
		void startAndEndDateQueryParameters(VaultClient vaultClient) {

			EmailNotificationHistoryResponse response = vaultClient.newRequest(LogRequest.class)
					.setStartDate(LocalDate.now().minusDays(30))
					.setEndDate(LocalDate.now().minusDays(1))
					.retrieveEmailNotificationHistory();

			System.out.println("Response Status: " + response.getResponseStatus());
			System.out.println("Response Message: " + response.getResponse());
			assertTrue(response.isSuccessful());

			EmailNotificationHistoryResponse.ResponseDetails details = response.getResponseDetails();
			System.out.println("Response Details ****");
			System.out.println("Offset = " + details.getOffset());
			System.out.println("Limit = " + details.getLimit());
			System.out.println("Size = " + details.getSize());
			System.out.println("Total = " + details.getTotal());

			System.out.println("Items ****");
			for (EmailNotification data : response.getData()) {
				System.out.println("id = " + data.getNotificationId());
				System.out.println("Send Date: " + data.getSendDate());
				System.out.println("Recipient Email: " + data.getRecipientEmail());
				assertNotNull(data.getNotificationId());
				assertNotNull(data.getSendDate());
			}
		}

		@Test
		@DisplayName("with all_dates = true query parameter")
		void allDatesEqualsTrueQueryParameters(VaultClient vaultClient) {

			JobCreateResponse response = vaultClient.newRequest(LogRequest.class)
					.setAllDates(true)
					.setFormatResult(LogRequest.FormatResultType.CSV)
					.retrieveEmailNotificationHistory();

//			This will only work once every 24 hours
			assertTrue(response.isSuccessful());
			System.out.println("Response Status: " + response.getResponseStatus());
			System.out.println("Response Message: " + response.getResponse());
			System.out.println("Job ID: " + response.getJobId());

		}
	}


	@Test
	public void testRetrieveDailyAPIUsageToFile(VaultClient vaultClient) {
		// Get yesterdays logs
		LocalDate date = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1).toLocalDate();

		// Set output file path
		Path outputFilePath = Paths.get(System.getProperty("user.home"), "Desktop", "response.zip");

		VaultResponse response = vaultClient.newRequest(LogRequest.class)
				.setOutputPath(outputFilePath.toString())
				.retrieveDailyAPIUsage(date);

		Assertions.assertTrue(response.isSuccessful());
		Assertions.assertNotNull(response.getOutputFilePath());
	}

	@Test
	public void testRetrieveDailyAPIUsageToBytes(VaultClient vaultClient) {
		// Get yesterdays logs
		LocalDate date = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1).toLocalDate();

		// Set output file path
		Path outputFilePath = Paths.get(System.getProperty("user.home"), "Desktop", "response.zip");

		// Retrieve the Zip file as bytes in the response
		// Be sure and call setOutputPath(null) here. This is a shared value and can be used in multiple reqs,
		// so safest to set to null in case another call set this value
		VaultResponse response = vaultClient.newRequest(LogRequest.class)
				.setLogFormat(LogRequest.LogFormatType.LOGFILE)
				.setOutputPath(null)
				.retrieveDailyAPIUsage(date);

		Assertions.assertTrue(response.isSuccessful());
		if (response.getResponseStatus().equals(VaultResponse.HTTP_RESPONSE_SUCCESS)) {
			try (OutputStream os = new FileOutputStream(outputFilePath.toString())) {
				os.write(response.getBinaryContent());
			}
			catch (IOException ignored){}
		}
	}

	@Test
	public void testDownloadSdkRuntimeLogsToFile(VaultClient vaultClient) {
		// Get yesterdays logs
		LocalDate date = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1).toLocalDate();

		// Set output file path
		Path outputFilePath = Paths.get(System.getProperty("user.home"), "Desktop", "response.zip");

		VaultResponse response = vaultClient.newRequest(LogRequest.class)
				.setOutputPath(outputFilePath.toString())
				.downloadSdkRuntimeLog(date);

		Assertions.assertTrue(response.isSuccessful());
		Assertions.assertNotNull(response.getOutputFilePath());
	}

	@Test
	public void testDownloadSdkRuntimeLogsToBytes(VaultClient vaultClient) {
		// Get yesterdays logs
		LocalDate date = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1).toLocalDate();

		// Set output file path
		Path outputFilePath = Paths.get(System.getProperty("user.home"), "Desktop", "response.zip");

		// Retrieve the Zip file as bytes in the response
		// Be sure and call setOutputPath(null) here. This is a shared value and can be used in multiple reqs,
		// so safest to set to null in case another call set this value
		VaultResponse response = vaultClient.newRequest(LogRequest.class)
				.setLogFormat(LogRequest.LogFormatType.LOGFILE)
				.setOutputPath(null)
				.downloadSdkRuntimeLog(date);

		Assertions.assertTrue(response.isSuccessful());
		if (response.getResponseStatus().equals(VaultResponse.HTTP_RESPONSE_SUCCESS)) {
			try (OutputStream os = new FileOutputStream(outputFilePath.toString())) {
				os.write(response.getBinaryContent());
			}
			catch (IOException ignored){}
		}
	}
}
