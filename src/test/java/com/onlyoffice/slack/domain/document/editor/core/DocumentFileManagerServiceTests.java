package com.onlyoffice.slack.domain.document.editor.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hazelcast.map.IMap;
import com.onlyoffice.model.common.Format;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.onlyoffice.slack.domain.document.DocumentServerFormatsConfiguration;
import com.onlyoffice.slack.shared.configuration.ServerConfigurationProperties;
import com.onlyoffice.slack.shared.transfer.cache.DocumentSessionKey;
import com.slack.api.model.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentFileManagerServiceTests {
  @Mock ServerConfigurationProperties serverConfigurationProperties;
  @Mock DocumentServerFormatsConfiguration formatsConfiguration;

  @Mock DocumentFileKeyBuilder documentFileKeyBuilder;

  @Mock IMap<String, DocumentSessionKey> keys;
  @Mock DocumentJwtManagerService documentJwtManagerService;
  @InjectMocks DocumentFileManagerServiceImpl documentFileManagerService;

  @Test
  void whenGetExtensionWithValidFile_thenReturnExtension() {
    var file = new File();
    file.setName("test.docx");

    assertEquals("docx", documentFileManagerService.getExtension(file));
  }

  @Test
  void whenGetExtensionWithNoExtension_thenReturnNull() {
    var file = new File();
    file.setName("testfile");

    assertNull(documentFileManagerService.getExtension(file));
  }

  @Test
  void whenGetDocumentTypeWithKnownExtension_thenReturnType() {
    var file = new File();
    file.setName("test.docx");

    var format = mock(Format.class);

    when(format.getName()).thenReturn("docx");
    when(format.getType()).thenReturn(DocumentType.WORD);
    when(formatsConfiguration.getFormats()).thenReturn(List.of(format));
    assertEquals(DocumentType.WORD, documentFileManagerService.getDocumentType(file));
  }

  @Test
  void whenIsEditableWithEditableFile_thenReturnTrue() {
    var file = new File();
    file.setName("test.docx");
    var format = mock(Format.class);

    when(format.getName()).thenReturn("docx");
    when(format.getActions()).thenReturn(List.of("edit"));
    when(formatsConfiguration.getFormats()).thenReturn(List.of(format));
    assertTrue(documentFileManagerService.isEditable(file));
  }
}
