package com.example;

import com.example.mymod.modules.TargetAssistant;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Biz yaratgan KillAura modulini shu yerda e'lon qilamiz
    public static TargetAssistant targetAssistant;

    @Override
    public void onInitialize() {
        LOGGER.info("Soft Client yuklanmoqda...");
        
        // O'yin ishga tushganda modulimizni faollashtiramiz
        targetAssistant = new TargetAssistant();
    }
}
