/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.model;

import org.gradle.api.DomainObjectSet;
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.InvalidUserCodeException;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.NamedDomainObjectList;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.collections.DomainObjectCollectionFactory;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.internal.file.FilePropertyFactory;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.collections.DirectoryFileTreeFactory;
import org.gradle.api.internal.provider.DefaultListProperty;
import org.gradle.api.internal.provider.DefaultMapProperty;
import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.internal.provider.DefaultSetProperty;
import org.gradle.api.internal.provider.PropertyHost;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.reflect.ObjectInstantiationException;
import org.gradle.internal.Cast;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.reflect.JavaReflectionUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultObjectFactory implements ObjectFactory {
    private final Instantiator instantiator;
    private final NamedObjectInstantiator namedObjectInstantiator;
    private final FileResolver fileResolver;
    private final DirectoryFileTreeFactory directoryFileTreeFactory;
    private final PropertyHost propertyHost;
    private final FilePropertyFactory filePropertyFactory;
    private final FileCollectionFactory fileCollectionFactory;
    private final DomainObjectCollectionFactory domainObjectCollectionFactory;

    public DefaultObjectFactory(Instantiator instantiator, NamedObjectInstantiator namedObjectInstantiator, FileResolver fileResolver, DirectoryFileTreeFactory directoryFileTreeFactory,
                                PropertyHost propertyHost, FilePropertyFactory filePropertyFactory, FileCollectionFactory fileCollectionFactory,
                                DomainObjectCollectionFactory domainObjectCollectionFactory) {
        this.instantiator = instantiator;
        this.namedObjectInstantiator = namedObjectInstantiator;
        this.fileResolver = fileResolver;
        this.directoryFileTreeFactory = directoryFileTreeFactory;
        this.propertyHost = propertyHost;
        this.filePropertyFactory = filePropertyFactory;
        this.fileCollectionFactory = fileCollectionFactory;
        this.domainObjectCollectionFactory = domainObjectCollectionFactory;
    }

    @Override
    public <T extends Named> T named(final Class<T> type, final String name) {
        return namedObjectInstantiator.named(type, name);
    }

    @Override
    public <T> T newInstance(Class<? extends T> type, Object... parameters) throws ObjectInstantiationException {
        return instantiator.newInstance(type, parameters);
    }

    @Override
    public ConfigurableFileCollection fileCollection() {
        return fileCollectionFactory.configurableFiles();
    }

    @Override
    public ConfigurableFileTree fileTree() {
        return fileCollectionFactory.fileTree();
    }

    @Override
    public SourceDirectorySet sourceDirectorySet(final String name, final String displayName) {
        return new DefaultSourceDirectorySet(name, displayName, fileResolver.getPatternSetFactory(), fileCollectionFactory, directoryFileTreeFactory, DefaultObjectFactory.this);
    }

    @Override
    public DirectoryProperty directoryProperty() {
        return filePropertyFactory.newDirectoryProperty();
    }

    @Override
    public RegularFileProperty fileProperty() {
        return filePropertyFactory.newFileProperty();
    }

    @Override
    public <T> NamedDomainObjectContainer<T> domainObjectContainer(Class<T> elementType) {
        return domainObjectCollectionFactory.newNamedDomainObjectContainer(elementType);
    }

    @Override
    public <T> NamedDomainObjectContainer<T> domainObjectContainer(Class<T> elementType, NamedDomainObjectFactory<T> factory) {
        return domainObjectCollectionFactory.newNamedDomainObjectContainer(elementType, factory);
    }

    @Override
    public <T> ExtensiblePolymorphicDomainObjectContainer<T> polymorphicDomainObjectContainer(Class<T> elementType) {
        return domainObjectCollectionFactory.newPolymorphicDomainObjectContainer(elementType);
    }

    @Override
    public <T> NamedDomainObjectSet<T> namedDomainObjectSet(Class<T> elementType) {
        return domainObjectCollectionFactory.newNamedDomainObjectSet(elementType);
    }

    @Override
    public <T> NamedDomainObjectList<T> namedDomainObjectList(Class<T> elementType) {
        return domainObjectCollectionFactory.newNamedDomainObjectList(elementType);
    }

    @Override
    public <T> DomainObjectSet<T> domainObjectSet(Class<T> elementType) {
        return domainObjectCollectionFactory.newDomainObjectSet(elementType);
    }

    @Override
    public <T> Property<T> property(Class<T> valueType) {
        if (valueType == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }

        if (valueType.isPrimitive()) {
            // Kotlin passes these types for its own basic types
            return Cast.uncheckedNonnullCast(property(JavaReflectionUtil.getWrapperTypeForPrimitiveType(valueType)));
        }
        if (List.class.isAssignableFrom(valueType)) {
            throw new InvalidUserCodeException(invalidPropertyCreationError("listProperty()", "List<T>"));
        } else if (Set.class.isAssignableFrom(valueType)) {
            throw new InvalidUserCodeException(invalidPropertyCreationError("setProperty()", "Set<T>"));
        } else if (Map.class.isAssignableFrom(valueType)) {
            throw new InvalidUserCodeException(invalidPropertyCreationError("mapProperty()", "Map<K, V>"));
        } else if (Directory.class.isAssignableFrom(valueType)) {
            throw new InvalidUserCodeException(invalidPropertyCreationError("directoryProperty()", "Directory"));
        } else if (RegularFile.class.isAssignableFrom(valueType)) {
            throw new InvalidUserCodeException(invalidPropertyCreationError("fileProperty()", "RegularFile"));
        }

        return new DefaultProperty<>(propertyHost, valueType);
    }

    private String invalidPropertyCreationError(String correctMethodName, String propertyType) {
        return "Please use the ObjectFactory." + correctMethodName + " method to create a property of type " + propertyType + ".";
    }

    @Override
    public <T> ListProperty<T> listProperty(Class<T> elementType) {
        if (elementType.isPrimitive()) {
            // Kotlin passes these types for its own basic types
            return Cast.uncheckedNonnullCast(listProperty(JavaReflectionUtil.getWrapperTypeForPrimitiveType(elementType)));
        }
        return new DefaultListProperty<>(propertyHost, elementType);
    }

    @Override
    public <T> SetProperty<T> setProperty(Class<T> elementType) {
        if (elementType.isPrimitive()) {
            // Kotlin passes these types for its own basic types
            return Cast.uncheckedNonnullCast(setProperty(JavaReflectionUtil.getWrapperTypeForPrimitiveType(elementType)));
        }
        return new DefaultSetProperty<>(propertyHost, elementType);
    }

    @Override
    public <K, V> MapProperty<K, V> mapProperty(Class<K> keyType, Class<V> valueType) {
        if (keyType.isPrimitive()) {
            // Kotlin passes these types for its own basic types
            return Cast.uncheckedNonnullCast(mapProperty(JavaReflectionUtil.getWrapperTypeForPrimitiveType(keyType), valueType));
        }
        if (valueType.isPrimitive()) {
            // Kotlin passes these types for its own basic types
            return Cast.uncheckedNonnullCast(mapProperty(keyType, JavaReflectionUtil.getWrapperTypeForPrimitiveType(valueType)));
        }
        return new DefaultMapProperty<>(propertyHost, keyType, valueType);
    }
}