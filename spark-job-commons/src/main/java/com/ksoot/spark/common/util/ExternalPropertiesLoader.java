package com.ksoot.spark.common.util;

import com.google.common.io.Files;
import java.io.IOException;
import java.util.Properties;
import lombok.experimental.UtilityClass;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;

@UtilityClass
public class ExternalPropertiesLoader extends PropertiesLoaderUtils {

  private static final String PROPERTY_FILE_EXTENSION = "properties";
  private static final String CONF_FILE_EXTENSION = "conf";
  private static final String XML_FILE_EXTENSION = "xml";
  private static final String YML_FILE_EXTENSION = "yml";
  private static final String YAML_FILE_EXTENSION = "yaml";

  private static final PropertiesPropertySourceLoader propLoader =
      new PropertiesPropertySourceLoader();

  private static final YamlPropertySourceLoader ymlLoader = new YamlPropertySourceLoader();

  public static PropertySource<?> loadPropertySource(final String prefix, final String filename)
      throws IOException {
    Assert.hasLength(filename, "Resource file must not be null");
    Resource path = new ClassPathResource(filename);
    Assert.isTrue(path.exists(), "Resource " + path + " does not exist");
    String fileNameWithoutExtension = Files.getNameWithoutExtension(filename);
    String extension = Files.getFileExtension(filename);
    if (extension.contentEquals(PROPERTY_FILE_EXTENSION)
        || extension.contentEquals(CONF_FILE_EXTENSION)
        || extension.contentEquals(XML_FILE_EXTENSION)) {
      return propLoader.load(prefix + fileNameWithoutExtension, path).get(0);
    } else if (extension.contentEquals(YML_FILE_EXTENSION)
        || extension.contentEquals(YAML_FILE_EXTENSION)) {
      return ymlLoader.load(prefix + fileNameWithoutExtension, path).get(0);
    } else {
      throw new IllegalArgumentException(
          "Resource " + filename + " does not have a valid extension: " + extension);
    }
  }

  public static PropertySource<?> loadPropertySource(final String sourceName, final Resource file)
      throws IOException {
    Assert.hasLength(sourceName, "String sourceName must not be null");
    Assert.notNull(file, "Resource file must not be null");
    Assert.isTrue(file.exists(), "Resource " + file + " does not exist");
    String extension = Files.getFileExtension(file.getFilename());
    if (extension.contentEquals(PROPERTY_FILE_EXTENSION)
        || extension.contentEquals(XML_FILE_EXTENSION)) {
      return propLoader.load(sourceName, file).get(0);
    } else if (extension.contentEquals(YML_FILE_EXTENSION)
        || extension.contentEquals(YAML_FILE_EXTENSION)) {
      return ymlLoader.load(sourceName, file).get(0);
    } else {
      throw new IllegalArgumentException(
          "Resource " + file + " does not have a valid extension: " + extension);
    }
  }

  public static PropertySource<?> loadYamlPropertySource(
      final String sourceName, final Resource file) throws IOException {
    Assert.hasLength(sourceName, "String sourceName must not be null");
    Assert.notNull(file, "Resource file must not be null");
    Assert.isTrue(file.exists(), "Resource " + file + " does not exist");
    return ymlLoader.load(sourceName, file).get(0);
  }

  public static PropertySource<?> loadPropertiesPropertySource(
      final String sourceName, final Resource file) throws IOException {
    Assert.hasLength(sourceName, "String sourceName must not be null");
    Assert.notNull(file, "Resource file must not be null");
    Assert.isTrue(file.exists(), "Resource " + file + " does not exist");
    return propLoader.load(sourceName, file).get(0);
  }

  public static Properties loadProperties(final Resource file) throws IOException {
    Assert.notNull(file, "Resource file must not be null");
    Assert.isTrue(file.exists(), "Resource " + file + " does not exist");
    String extension = Files.getFileExtension(file.getFilename());
    if (extension.contentEquals(PROPERTY_FILE_EXTENSION)
        || extension.contentEquals(CONF_FILE_EXTENSION)
        || extension.contentEquals(XML_FILE_EXTENSION)) {
      return PropertiesLoaderUtils.loadProperties(file);
    } else if (extension.contentEquals(YML_FILE_EXTENSION)
        || extension.contentEquals(YAML_FILE_EXTENSION)) {
      YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
      factory.setResources(file);
      factory.afterPropertiesSet();
      return factory.getObject();
    } else {
      throw new IllegalArgumentException(
          "Resource " + file + " does not have a valid extension: " + extension);
    }
  }

  public static Properties loadProperties(final String fileName) throws IOException {
    Assert.hasLength(fileName, "fileName including path required");
    Resource file = new ClassPathResource(fileName);
    return loadProperties(file);
  }
}
