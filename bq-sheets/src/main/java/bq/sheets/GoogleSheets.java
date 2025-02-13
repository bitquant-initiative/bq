package bq.sheets;

import bq.util.BqException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.common.base.Suppliers;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class GoogleSheets {

  FluentLogger logger = FluentLogger.forEnclosingClass();

  private static GoogleSheets singleton;

  static final YAMLMapper yamlMapper = new YAMLMapper();
  Sheets sheets;

  public String resolveId(String id) {

    try {
      File f = new File(System.getProperty("user.home"), ".bq/config.yml");

      JsonNode n = yamlMapper.readTree(f);
      JsonNode x = n.path("sheets").path(id).path("id");

      if (x.isTextual()) {
        return x.asText();
      }

    } catch (IOException e) {

    }
    return id;
  }

  public GoogleSpreadsheet getSheet(String id) {

    try {
      Spreadsheet spreadsheet = getService().spreadsheets().get(resolveId(id)).execute();

      GoogleSpreadsheet gs = new GoogleSpreadsheet();
      gs.spreadsheet = spreadsheet;
      return gs;
    } catch (IOException e) {
      throw new BqException(e);
    }
  }

  public static GoogleSheets get() {
    if (singleton == null) {
      singleton = GoogleSheets.builder().build();
    }
    return singleton;
  }

  static class Builder {
    Supplier<File> credentialsFileSupplier = Suppliers.memoize(GoogleSheets::findCredentialsFile);
    Supplier<File> tokensDirSupplier = Suppliers.memoize(GoogleSheets::findTokensDir);

    List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    String applicationName = "bq";

    public GoogleSheets build() {

      try {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Sheets service =
            new Sheets.Builder(
                    HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), getCredential(HTTP_TRANSPORT))
                .setApplicationName(applicationName)
                .build();
        GoogleSheets sheets = new GoogleSheets();
        sheets.sheets = service;
        return sheets;
      } catch (IOException | GeneralSecurityException e) {
        throw new BqException(e);
      }
    }

    public Credential getCredential(HttpTransport transport) {

      try (Closer closer = Closer.create()) {
        Reader in =
            new InputStreamReader(
                Files.asByteSource(credentialsFileSupplier.get()).openBufferedStream());
        closer.register(in);

        GsonFactory gsonFactory = GsonFactory.getDefaultInstance();

        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), in);

        GoogleAuthorizationCodeFlow flow =
            new GoogleAuthorizationCodeFlow.Builder(transport, gsonFactory, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(tokensDirSupplier.get()))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
      } catch (IOException e) {
        throw new BqException(e);
      }
    }
  }

  public Sheets getService() {
    return sheets;
  }

  public static Builder builder() {

    return new Builder();
  }

  static File findCredentialsFile() {
    String home = System.getProperty("user.home");

    File bqSheetsDir = new File(home, ".bq/sheets");

    File credentials = new File(bqSheetsDir, "credentials.json");
    return credentials;
  }

  static File findTokensDir() {
    String home = System.getProperty("user.home");

    File tokensDir = new File(home, ".bq/sheets/tokens");

    return tokensDir;
  }
}
