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
        log.debug("validating file {} size", file.getName());
        return ((double) file.getSize() / (1024 * 1024)) <= integrationConfiguration.getFileSizeLimitMb();
    }

    public String convertFileSize(File file) {
        log.debug("converting file {} size: {}", file.getName(), file.getSize());
        return decimalFormat.format((double) file.getSize() / (1024 * 1024)) + " mb";
    }

    public String convertFileTimestamp(File file) {
        log.debug("converting file {} timestamp: {}", file.getName(), file.getTimestamp());
        SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return f.format(new Date((long) file.getTimestamp() * 1000));
    }

    public String covertDownloadName(File file) {
        log.debug("converting file {} download name", file.getName());
        int downloadNameIndex = file.getUrlPrivateDownload().lastIndexOf("/")+1;
        return file.getUrlPrivateDownload().substring(downloadNameIndex);
    }

    public String convertFileIconUrl(File file) {
        log.debug("converting file {} icon url", file.getName());
        return String.format("%s.png", integrationConfiguration.getIconsBaseUrl() + fileUtil.findFileType(file.getName()));
    }
}
