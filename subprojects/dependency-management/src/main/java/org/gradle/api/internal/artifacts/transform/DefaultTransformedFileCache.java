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

package org.gradle.api.internal.artifacts.transform;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.gradle.api.Transformer;
import org.gradle.internal.UncheckedException;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class DefaultTransformedFileCache implements TransformedFileCache {
    private final Cache<Key, List<File>> results = CacheBuilder.newBuilder().build();

    @Override
    public Transformer<List<File>, File> applyCaching(final Class<?> implementationClass, final Object[] params, final Transformer<List<File>, File> transformer) {
        return new Transformer<List<File>, File>() {
            @Override
            public List<File> transform(final File file) {
                Key key = new Key(file, implementationClass, params);
                try {
                    return results.get(key, new Callable<List<File>>() {
                        @Override
                        public List<File> call() {
                            return ImmutableList.copyOf(transformer.transform(file));
                        }
                    });
                } catch (ExecutionException e) {
                    throw UncheckedException.throwAsUncheckedException(e.getCause());
                } catch (UncheckedExecutionException e) {
                    throw UncheckedException.throwAsUncheckedException(e.getCause());
                }
            }
        };
    }

    private static class Key {
        final File file;
        // TODO - don't retain strong reference to class and parameter values
        final Class<?> implementationClass;
        // TODO - don't cache potentially mutable values
        final Object[] parameters;

        public Key(File file, Class<?> implementationClass, Object[] parameters) {
            this.file = file;
            this.implementationClass = implementationClass;
            this.parameters = parameters;
        }

        @Override
        public boolean equals(Object obj) {
            Key other = (Key) obj;
            return file.equals(other.file) && implementationClass.equals(other.implementationClass) && Arrays.equals(parameters, other.parameters);
        }

        @Override
        public int hashCode() {
            return file.hashCode() ^ implementationClass.hashCode() ^ Arrays.hashCode(parameters);
        }
    }
}
