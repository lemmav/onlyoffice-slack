package com.onlyoffice.slack.util;

import com.onlyoffice.slack.configuration.general.IntegrationConfiguration;
import com.slack.api.model.File;
import core.model.converter.format.DocumentType;
import core.util.OnlyofficeFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackFileConverter {
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    private final IntegrationConfiguration integrationConfiguration;
    private final OnlyofficeFile fileUtil;

    public boolean fileSizeAllowed(File file) {
        log.debug("Validating file {} size", file.getName());
        return ((double) file.getSize() / (1024 * 1024)) <= integrationConfiguration.getFileSizeLimitMb();
    }

    public String convertFileSize(File file) {
        log.debug("Converting file {} size: {}", file.getName(), file.getSize());
        return decimalFormat.format((double) file.getSize() / (1024 * 1024)) + " mb";
    }

    public String convertFileTimestamp(File file) {
        log.debug("Converting file {} timestamp: {}", file.getName(), file.getTimestamp());
        SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return f.format(new Date((long) file.getTimestamp() * 1000));
    }

    public String covertDownloadName(File file) {
        log.debug("Converting file {} download name", file.getName());
        int downloadNameIndex = file.getUrlPrivateDownload().lastIndexOf("/")+1;
        return file.getUrlPrivateDownload().substring(downloadNameIndex);
    }

    //TODO: Default icon
    public String convertFileIconUrl(File file) {
        log.debug("Converting file {} icon url", file.getName());
        DocumentType type = fileUtil.findDocumentType(file.getName());
        switch (type) {
            case WORD -> { return integrationConfiguration.getWordIcon(); }
            case CELL -> { return integrationConfiguration.getCellIcon(); }
            case SLIDE -> { return integrationConfiguration.getSlideIcon(); }
            default -> { return integrationConfiguration.getWordIcon(); }
        }
    }
}
