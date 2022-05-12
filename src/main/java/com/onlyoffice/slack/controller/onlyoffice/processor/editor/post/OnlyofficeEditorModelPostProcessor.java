package com.onlyoffice.slack.controller.onlyoffice.processor.editor.post;


import com.google.common.collect.ImmutableMap;
import core.model.config.Config;
import core.processor.postprocessor.OnlyofficeEditorPostProcessor;
import exception.OnlyofficeInvalidParameterRuntimeException;
import exception.OnlyofficeProcessAfterRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OnlyofficeEditorModelPostProcessor extends OnlyofficeEditorPostProcessor<Model> {
    public Model validateSchema(Map<String, Object> customData, ImmutableMap<String, Object> schema) {
        if (customData == null || !customData.containsKey("apijs")) return null;
        if (schema == null || !schema.containsKey("model")) return null;

        try {
            Model model = (Model) schema.get("model");
            if (model == null) return null;
            return model;
        } catch (ClassCastException e) {
            return null;
        }
    }

    public void processAfter(Config config, Model model) throws OnlyofficeProcessAfterRuntimeException, OnlyofficeInvalidParameterRuntimeException {
        Map<String, Object> custom = config.getCustom();
        model.addAttribute("config", config);
        model.addAttribute("apijs", custom.get("apijs").toString());
    }

    public String postprocessorName() {
        return "onlyoffice.postprocessor.slack.editor.model";
    }
}
