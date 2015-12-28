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

package org.dd4t.providers.impl;

import com.tridion.ItemTypes;
import com.tridion.content.BinaryFactory;
import com.tridion.data.BinaryData;
import com.tridion.dynamiccontent.DynamicMetaRetriever;
import com.tridion.meta.BinaryMeta;
import com.tridion.meta.BinaryMetaFactory;
import org.dd4t.contentmodel.Binary;
import org.dd4t.contentmodel.impl.BinaryDataImpl;
import org.dd4t.contentmodel.impl.BinaryImpl;
import org.dd4t.core.exceptions.ItemNotFoundException;
import org.dd4t.core.exceptions.SerializationException;
import org.dd4t.core.providers.BaseBrokerProvider;
import org.dd4t.core.util.TCMURI;
import org.dd4t.providers.BinaryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;

/**
 * Provides access to Binaries stored in the Content Delivery database. It uses JPA DAOs to retrieve raw binary content
 * or binary metadata from the database. Access to these objects is not cached, and as such must be cached externally.
 */
public class BrokerBinaryProvider extends BaseBrokerProvider implements BinaryProvider {

	private static final BinaryMetaFactory BINARY_META_FACTORY = new BinaryMetaFactory();

	private static final Logger LOG = LoggerFactory.getLogger(BrokerBinaryProvider.class);

	@Override public Binary getBinaryByURI (final String tcmUri) throws ItemNotFoundException, ParseException, SerializationException {
		final TCMURI binaryUri = new TCMURI(tcmUri);
		final BinaryMeta binaryMeta = BINARY_META_FACTORY.getMeta(tcmUri);
		return getBinary(binaryUri,binaryMeta);
	}


	@Override public Binary getBinaryByURL (final String url, final int publication) throws ItemNotFoundException, SerializationException {
		final BinaryMeta binaryMeta = BINARY_META_FACTORY.getMetaByURL(publication,url);
		final TCMURI binaryUri = new TCMURI(binaryMeta.getPublicationId(),TCMURI.safeLongToInt(binaryMeta.getId()), ItemTypes.COMPONENT,-1);
		return getBinary(binaryUri,binaryMeta);
	}

	private static Binary getBinary (final TCMURI binaryUri, final BinaryMeta binaryMeta) throws ItemNotFoundException {
		if (binaryMeta != null) {
			final BinaryImpl binary = new BinaryImpl();
			binary.setId(binaryUri.toString());
			binary.setUrlPath(binaryMeta.getURLPath());
			// TODO: check if this actually is the Mime Type
			binary.setMimeType(binaryMeta.getType());

			// TODO: binaryMeta.getCustomMeta();
			//binaryMeta.getDescription();
			//binaryMeta.getPath();
			//binaryMeta.getVariantId();

			BinaryData content = new BinaryFactory().getBinary(binaryUri.getPublicationId(), binaryUri.getItemId(), binaryMeta.getVariantId());
			if (content == null) {
				throw new ItemNotFoundException("Unable to find binary content by id:" + binaryUri.toString());
			}

			final BinaryDataImpl binaryData = new BinaryDataImpl();
			try {
				binaryData.setBytes(content.getBytes().clone());
			} catch (IOException e) {
				throw new ItemNotFoundException("Error reading binary content by id:" + binaryUri.toString());
			}
			binary.setBinaryData(binaryData);
			return binary;
		}
		return null;
	}

	/**
	 * Retrieves the byte array content of a Tridion binary based on its TCM item id and publication id.
	 *
	 * @param id          int representing the item id
	 * @param publication int representing the publication id
	 * @return byte[] the byte array of the binary content
	 * @throws ItemNotFoundException if the item identified by id and publication was not found
	 */
	@Override
	public byte[] getBinaryContentById (int id, int publication) throws ItemNotFoundException {
		BinaryData content = new BinaryFactory().getBinary(id, publication);

		if (content == null) {
			throw new ItemNotFoundException("Unable to find binary content by id '" + id + "' and publication '" + publication + "'.");
		}

		try {
			return content.getBytes();
		} catch (IOException e) {
			throw new ItemNotFoundException("Error reading binary content by id:" + id);
		}
	}

	/**
	 * Retrieves the byte array content of a Tridion binary based on its URL.
	 *
	 * @param url         string representing the path portion of the URL of the binary
	 * @param publication int representing the publication id
	 * @return byte[] the byte array of the binary content
	 * @throws ItemNotFoundException if the item identified by id and publication was not found
	 */
	@Override
	public byte[] getBinaryContentByURL (String url, int publication) throws ItemNotFoundException {

		BinaryMeta binaryMeta = new DynamicMetaRetriever().getBinaryMetaByURL(url);
		return getBinaryContentById((int) binaryMeta.getId(), publication);
	}

}
