/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.security.fixtures

import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.openpgp.PGPPublicKey
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.server.http.HttpServer

import static org.gradle.security.internal.SecuritySupport.toHexString

class KeyServer extends HttpServer {

    private final TestFile baseDirectory
    private final Map<String, File> keyFiles = [:]

    KeyServer(TestFile baseDirectory) {
        this.baseDirectory = baseDirectory
        allow("/pks/lookup", false, ["GET"], new HttpServer.ActionSupport("Get key") {
            @Override
            void handle(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) {
                if (request.queryString.startsWith("op=get&options=mr&search=0x")) {
                    String keyId = request.queryString - "op=get&options=mr&search=0x"
                    if (keyFiles.containsKey(keyId)) {
                        fileHandler("/pks/lookup", keyFiles[keyId]).handle(request, response)
                    } else {
                        response.sendError(404, "not found")
                    }
                }
            }
        })
    }

    void registerPublicKey(PGPPublicKey key) {
        String keyId = toHexString(key.keyID)
        def keyFile = baseDirectory.createFile("${keyId}.asc")
        keyFile.deleteOnExit()
        keyFile.newOutputStream().withCloseable { out ->
            new ArmoredOutputStream(out).withCloseable {
                key.encode(it)
            }
        }
        registerKey(keyId, keyFile)
    }

    private void registerKey(String keyId, File keyFile) {
        keyFiles[keyId] = keyFile
    }

    void withDefaultSigningKey() {
        File dir = baseDirectory.createDir("default-key")
        File publicKeyFile = new File(dir, "public-key.asc")
        publicKeyFile.deleteOnExit()
        SigningFixtures.writeValidPublicKeyTo(publicKeyFile)
        registerKey(SigningFixtures.validPublicKeyHexString, publicKeyFile)
    }

}
