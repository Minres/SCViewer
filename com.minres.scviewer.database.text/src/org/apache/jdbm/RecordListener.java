/*******************************************************************************
 * Copyright 2010 Cees De Groot, Alex Boisvert, Jan Kotek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.jdbm;

import java.io.IOException;

/**
 * An listener notifed when record is inserted, updated or removed.
 * <p/>
 * NOTE: this class was used in JDBM2 to support secondary indexes
 * JDBM3 does not have a secondary indexes, so this class is not publicly exposed.
 *
 * @param <K> key type
 * @param <V> value type
 * @author Jan Kotek
 */
interface RecordListener<K, V> {

    void recordInserted(K key, V value) throws IOException;

    void recordUpdated(K key, V oldValue, V newValue) throws IOException;

    void recordRemoved(K key, V value) throws IOException;

}
