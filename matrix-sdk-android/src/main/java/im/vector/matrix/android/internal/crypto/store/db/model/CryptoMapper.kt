/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.matrix.android.internal.crypto.store.db.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.crypto.crosssigning.DeviceTrustLevel
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.di.SerializeNulls
import timber.log.Timber

object CryptoMapper {

    private val moshi = Moshi.Builder().add(SerializeNulls.JSON_ADAPTER_FACTORY).build()
    private val listMigrationAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(
            List::class.java,
            String::class.java,
            Any::class.java
    ))
    private val mapMigrationAdapter = moshi.adapter<JsonDict>(Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
    ))
    private val mapOfStringMigrationAdapter = moshi.adapter<Map<String, Map<String, String>>>(Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
    ))

    internal fun mapToEntity(deviceInfo: CryptoDeviceInfo): DeviceInfoEntity {
        return DeviceInfoEntity(
                primaryKey = DeviceInfoEntity.createPrimaryKey(deviceInfo.userId, deviceInfo.deviceId),
                userId = deviceInfo.userId,
                deviceId = deviceInfo.deviceId,
                algorithmListJson = listMigrationAdapter.toJson(deviceInfo.algorithms),
                keysMapJson = mapMigrationAdapter.toJson(deviceInfo.keys),
                signatureMapJson = mapMigrationAdapter.toJson(deviceInfo.signatures),
                isBlocked = deviceInfo.isBlocked,
                trustLevelEntity = deviceInfo.trustLevel?.let {
                    TrustLevelEntity(
                            crossSignedVerified = it.crossSigningVerified,
                            locallyVerified = it.locallyVerified
                    )
                },
                unsignedMapJson = mapMigrationAdapter.toJson(deviceInfo.unsigned)
        )
    }

    internal fun mapToModel(deviceInfoEntity: DeviceInfoEntity): CryptoDeviceInfo {
        return CryptoDeviceInfo(
                userId = deviceInfoEntity.userId ?: "",
                deviceId = deviceInfoEntity.deviceId ?: "",
                isBlocked = deviceInfoEntity.isBlocked ?: false,
                trustLevel = deviceInfoEntity.trustLevelEntity?.let {
                    DeviceTrustLevel(it.crossSignedVerified ?: false, it.locallyVerified)
                },
                unsigned = deviceInfoEntity.unsignedMapJson?.let {
                    try {
                        mapMigrationAdapter.fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                signatures = deviceInfoEntity.signatureMapJson?.let {
                    try {
                        mapOfStringMigrationAdapter.fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                keys = deviceInfoEntity.keysMapJson?.let {
                    try {
                        moshi.adapter<Map<String, String>>(Types.newParameterizedType(
                                Map::class.java,
                                String::class.java,
                                Any::class.java
                        )).fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                },
                algorithms = deviceInfoEntity.algorithmListJson?.let {
                    try {
                        listMigrationAdapter.fromJson(it)
                    } catch (failure: Throwable) {
                        Timber.e(failure)
                        null
                    }
                }
        )
    }
}
