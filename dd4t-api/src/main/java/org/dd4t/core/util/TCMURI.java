/*
 * Copyright (c) 2015 SDL, Radagio & R. Oudshoorn
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

package org.dd4t.core.util;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import static org.dd4t.core.util.TCMURI.Namespace.ISH;
import static org.dd4t.core.util.TCMURI.Namespace.TCM;
import static org.dd4t.core.util.TCMURI.Namespace.getNamespaceFor;

public class TCMURI implements Serializable {

    public static final String URI_NAMESPACE = "tcm:";
    protected static final String SEPARATOR = "-";
    protected static final String DELIM_VERSION = "v";

    private int itemType;
    private int itemId;
    private int pubId;
    private int version;
    private Namespace namespace = TCM;

    public TCMURI() {
    }

    public TCMURI(String uri) throws ParseException {
        this.itemType = 0;
        this.itemId = -1;
        this.pubId = -1;
        this.version = -1;
        load(uri);
    }

    public TCMURI(int publicationId, int itemId, int itemType, int version) {
        this.itemType = itemType;
        this.itemId = itemId;
        this.pubId = publicationId;
        this.version = version;
    }

    public TCMURI(Namespace namespace, int publicationId, int itemId, int itemType, int version) {
        this(publicationId, itemId, itemType, version);
        this.namespace = namespace;
    }

    public static boolean isValid(String tcmUri) {
        return tcmUri != null && getNamespaceFor(tcmUri.split(":")[0]) != null;

    }

    protected void load(String uriString) throws ParseException {
        if (!isValid(uriString)) {
            throw new ParseException("Invalid TCMURI string", 0);
        }
        String[] parts = uriString.split(":");
        int currentPosition = parts[0].length();
        if (parts.length != 2 || Namespace.getNamespaceFor(parts[0]) == null) {
            throw new ParseException("URI string " + uriString + " does not start with '" + TCM.getValue() + "' or '"
                    + ISH.getValue() + "'", currentPosition);
        }

        this.namespace = Namespace.getNamespaceFor(parts[0]);
        String uri = parts[1];
        StringTokenizer st = new StringTokenizer(uri, "-");
        if (st.countTokens() < 2) {
            throw new ParseException("URI string " + uriString + " does not contain enough ID's", currentPosition);
        }
        try {
            String token = st.nextToken();
            currentPosition += token.length();
            this.pubId = Integer.parseInt(token);

            token = st.nextToken();
            currentPosition += token.length();
            this.itemId = Integer.parseInt(token);

            if (!st.hasMoreTokens()) {
                this.itemType = 16;
            } else {
                token = st.nextToken();
                currentPosition += token.length();
                if (!token.startsWith(DELIM_VERSION)) {
                    this.itemType = Integer.parseInt(token);
                } else {
                    this.version = Integer.parseInt(token.substring(1, token.length()));
                    this.itemType = 16;
                }

                if (st.hasMoreTokens()) {
                    token = st.nextToken();
                    currentPosition += token.length();
                    this.version = Integer.parseInt(token.substring(1, token.length()));
                }
            }
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid ID (not an integer) in URI string " + uriString, currentPosition);
        }
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(namespace.getValue());
        sb.append(":");
        sb.append(this.pubId);
        sb.append(SEPARATOR);
        sb.append(this.itemId);
        sb.append(SEPARATOR);
        sb.append(this.itemType);
        return sb.toString();
    }

    public int getItemType() {
        return this.itemType;
    }


    public int getItemId() {
        return this.itemId;
    }


    public int getPublicationId() {
        return this.pubId;
    }

    public int getVersion() {
        return this.version;
    }

    public Namespace getUriNamespace() {
        return this.namespace;
    }

    public enum Namespace {
        TCM("tcm"),
        ISH("ish");

        private String value;

        private static final Map<String, Namespace> namespaces = Collections.unmodifiableMap(initialize());

        private static Map<String, Namespace> initialize() {
            Map<String, Namespace> result = new HashMap<>();
            for (Namespace n : Namespace.values()) {
                result.put(n.getValue(), n);
            }
            return result;
        }

        Namespace(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Namespace getNamespaceFor(String value) {
            return namespaces.get(value);
        }
    }
}