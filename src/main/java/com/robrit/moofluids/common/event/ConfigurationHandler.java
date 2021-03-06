/*
 * ConfigurationHandler.java
 *
 * Copyright (c) 2014-2017 TheRoBrit
 *
 * Moo-Fluids is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Moo-Fluids is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.robrit.moofluids.common.event;

import com.robrit.moofluids.common.entity.EntityTypeData;
import com.robrit.moofluids.common.ref.ConfigurationData;
import com.robrit.moofluids.common.ref.ModInformation;
import com.robrit.moofluids.common.util.EntityHelper;
import com.robrit.moofluids.common.util.LogHelper;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

public class ConfigurationHandler {

  private static Configuration configuration;
  private static File configFile;

  public static void init() {
    setConfiguration(new Configuration(configFile, ConfigurationData.CONFIG_VERSION, true));
  }

  public static void updateConfiguration() {
    handleOldConfig();
    updateGlobalConfiguration();
    updateFluidConfiguration();

    if (configuration.hasChanged()) configuration.save();
  }

  public static void updateGlobalConfiguration() {
    /* Category comments */
    configuration.addCustomCategoryComment(ConfigurationData.CATEGORY_GLOBAL,
                                           ConfigurationData.CATEGORY_GLOBAL_COMMENT);

    configuration.addCustomCategoryComment(ConfigurationData.CATEGORY_FLUIDS,
                                           ConfigurationData.CATEGORY_FLUIDS_COMMENT);

    /* General configuration */
    ConfigurationData.GLOBAL_FLUID_COW_SPAWN_RATE_VALUE =
        configuration.get(ConfigurationData.CATEGORY_GLOBAL,
                          ConfigurationData.GLOBAL_FLUID_COW_SPAWN_RATE_KEY,
                          ConfigurationData.GLOBAL_FLUID_COW_SPAWN_RATE_DEFAULT_VALUE,
                          ConfigurationData.GLOBAL_FLUID_COW_SPAWN_RATE_COMMENT).getInt();

    ConfigurationData.EVENT_ENTITIES_ENABLED_VALUE =
        configuration.get(ConfigurationData.CATEGORY_GLOBAL,
                          ConfigurationData.EVENT_ENTITIES_ENABLED_KEY,
                          ConfigurationData.EVENT_ENTITIES_ENABLED_DEFAULT_VALUE).getBoolean();
  }

  public static void updateFluidConfiguration() {
    boolean filterModeBlack = configuration.get(ConfigurationData.CATEGORY_FLUID_FILTER,
                                          ConfigurationData.FILTER_TYPE_KEY,
                                          ConfigurationData.FILTER_TYPE_DEFAULT,
                                          ConfigurationData.FILTER_TYPE_COMMENT).getBoolean();
    String[] filterList =configuration.get(ConfigurationData.CATEGORY_FLUID_FILTER,
                                           ConfigurationData.FILTER_LIST_KEY,
                                           ConfigurationData.FILTER_LIST_DEFAULT,
                                           ConfigurationData.FILTER_LIST_COMMENT).getStringList();

    for (final Fluid containableFluid : EntityHelper.getContainableFluids().values()) {
      final String containableFluidLocalizedName =
          containableFluid.getLocalizedName(new FluidStack(containableFluid, 0));
      final String entityName = ConfigurationData.CATEGORY_FLUIDS + "." + containableFluidLocalizedName + " " + "Cow";
      final EntityTypeData entityTypeData = new EntityTypeData();

      /* Check the pasture for old cows to save */
      final String oldEntityName = "Pasture." + containableFluidLocalizedName.toLowerCase() + " " + "cow";
      if(configuration.hasCategory(oldEntityName)) {
        configuration.moveProperty(oldEntityName, ConfigurationData.ENTITY_IS_SPAWNABLE_KEY, entityName);
        configuration.moveProperty(oldEntityName, ConfigurationData.ENTITY_SPAWN_RATE_KEY, entityName);
        configuration.moveProperty(oldEntityName, ConfigurationData.ENTITY_FIRE_DAMAGE_AMOUNT_KEY, entityName);
        configuration.moveProperty(oldEntityName, ConfigurationData.ENTITY_NORMAL_DAMAGE_AMOUNT_KEY, entityName);
        configuration.moveProperty(oldEntityName, ConfigurationData.ENTITY_GROW_UP_TIME_KEY, entityName);
        configuration.moveProperty(oldEntityName, ConfigurationData.ENTITY_MAX_USE_COOLDOWN_KEY, entityName);
        configuration.moveProperty(oldEntityName, ConfigurationData.ENTITY_CAN_DAMAGE_PLAYER_KEY, entityName);
        configuration.moveProperty(oldEntityName, ConfigurationData.ENTITY_CAN_DAMAGE_OTHER_ENTITIES_KEY, entityName);
      }

        /* Configurable entity data */
      entityTypeData.setSpawnable(
          configuration.get(entityName,
                            ConfigurationData.ENTITY_IS_SPAWNABLE_KEY,
                            ConfigurationData.ENTITY_IS_SPAWNABLE_DEFAULT_VALUE).getBoolean());
      entityTypeData.setSpawnRate(
          configuration.get(entityName,
                            ConfigurationData.ENTITY_SPAWN_RATE_KEY,
                            ConfigurationData.ENTITY_SPAWN_RATE_DEFAULT_VALUE).getInt());
      entityTypeData.setFireDamageAmount(
          configuration.get(entityName,
                            ConfigurationData.ENTITY_FIRE_DAMAGE_AMOUNT_KEY,
                            ConfigurationData.ENTITY_FIRE_DAMAGE_AMOUNT_DEFAULT_VALUE).getInt());
      entityTypeData.setNormalDamageAmount(
          configuration.get(entityName,
                            ConfigurationData.ENTITY_NORMAL_DAMAGE_AMOUNT_KEY,
                            ConfigurationData.ENTITY_NORMAL_DAMAGE_AMOUNT_DEFAULT_VALUE).getInt());
      entityTypeData.setGrowUpTime(
          configuration.get(entityName,
                            ConfigurationData.ENTITY_GROW_UP_TIME_KEY,
                            ConfigurationData.ENTITY_GROW_UP_TIME_DEFAULT_VALUE).getInt());
      entityTypeData.setMaxUseCooldown(
          configuration.get(entityName,
                            ConfigurationData.ENTITY_MAX_USE_COOLDOWN_KEY,
                            ConfigurationData.ENTITY_MAX_USE_COOLDOWN_DEFAULT_VALUE).getInt());
      entityTypeData.setDamagePlayers(
          configuration.get(entityName,
                            ConfigurationData.ENTITY_CAN_DAMAGE_PLAYER_KEY,
                            ConfigurationData.ENTITY_CAN_DAMAGE_PLAYER_DEFAULT_VALUE).getBoolean());
      entityTypeData.setDamageEntities(
          configuration.get(entityName,
                            ConfigurationData.ENTITY_CAN_DAMAGE_OTHER_ENTITIES_KEY,
                            ConfigurationData.ENTITY_CAN_DAMAGE_OTHER_ENTITIES_DEFAULT_VALUE).getBoolean());

      /* Non-configurable entity data */
      entityTypeData.setCauseFireDamage(entityTypeData.getFireDamageAmount() > 0);
      entityTypeData.setCauseNormalDamage(entityTypeData.getNormalDamageAmount() > 0);

      /* Override spawning based on filter */
      for (String filter: filterList) {
        if(filterModeBlack == (containableFluid.getName().contains(filter) ||
                               containableFluid.getUnlocalizedName().contains(filter) ||
                               containableFluidLocalizedName.contains(filter))) {
          entityTypeData.setSpawnable(false);
          break;
        }
      }

      EntityHelper.setEntityData(containableFluid.getName(), entityTypeData);
    }
    configuration.removeCategory(configuration.getCategory("Pasture"));
  }

  private static void handleOldConfig() {
    /* Look for a unique config arrangement from the original format */
    if (configuration.hasKey("fluid cow global spawn rate", "Fluid Cow Global Spawn Rate")) {
      // Migrate original config format
      configuration.moveProperty("fluid cow global spawn rate",
                       "Fluid Cow Global Spawn Rate",
                                 ConfigurationData.CATEGORY_GLOBAL);
      configuration.removeCategory(configuration.getCategory("fluid cow global spawn rate"));
      configuration.moveProperty("event entities enabled",
              "Event Entities Enabled",
              ConfigurationData.CATEGORY_GLOBAL);
      configuration.removeCategory(configuration.getCategory("event entities enabled"));

      /* Put all the old cows out to pasture. Will attempt to recover them during fluid config processing,
         and any that remain will be washed away.
       */
      for (final String Cow : configuration.getCategoryNames()) {
        if(Cow.endsWith("cow")) {
          for (final Property prop: configuration.getCategory(Cow).values()) {
            configuration.get("Pasture." + Cow, prop.getName(), prop.getString());
          }
          configuration.removeCategory(configuration.getCategory(Cow));
        }
      }
    }
    /* For future changes:
    if (configuration.getDefinedConfigVersion() != configuration.getLoadedConfigVersion()) {

    }
    */

  }

  public static Configuration getConfiguration() {
    return configuration;
  }

  public static void setConfiguration(Configuration newConfiguration) {
    configuration = newConfiguration;
  }

  public static File getConfigFile() {
    return configFile;
  }

  public static void setConfigFile(File newConfigFile) {
    configFile = newConfigFile;
  }

  @SubscribeEvent
  public static void onConfigurationChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
    if (event.getModID().equalsIgnoreCase(ModInformation.MOD_ID)) {
      updateConfiguration();
    }
  }
}
