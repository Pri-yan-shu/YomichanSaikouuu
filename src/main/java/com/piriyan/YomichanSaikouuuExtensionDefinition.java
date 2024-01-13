package com.piriyan;
import java.util.UUID;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public final class YomichanSaikouuuExtensionDefinition extends ControllerExtensionDefinition {
   private static final UUID DRIVER_ID = UUID.fromString("efdb3fac-26b2-4a72-a0a5-0869b598da69");
   
   public YomichanSaikouuuExtensionDefinition() {}

   @Override
   public String getName() {
      return "Yomichan Saikouuu";
   }
   
   @Override
   public String getAuthor() {
      return "Piriyan";
   }

   @Override
   public String getVersion() {
      return "0.1";
   }

   @Override
   public UUID getId() {
      return DRIVER_ID;
   }
   
   @Override
   public String getHardwareVendor() {
      return "Piriyan";
   }
   
   @Override
   public String getHardwareModel() {
      return "Yomichan Saikouuu";
   }

   @Override
   public int getRequiredAPIVersion() {
      return 18;
   }

   @Override
   public int getNumMidiInPorts() {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts() {
      return 0;
   }

   @Override
   public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType) {}

   @Override
   public YomichanSaikouuuExtension createInstance(final ControllerHost host) {
      return new YomichanSaikouuuExtension(this, host);
   }
}
