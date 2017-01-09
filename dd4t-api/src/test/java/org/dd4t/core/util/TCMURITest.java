package org.dd4t.core.util;

import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TCMURITest {
    public static final int PUBLICATION_ID = 12;
    public static final int ITEM_ID = 14;
    public static final int ITEM_TYPE = 16;
    public static final int VERSION = 2;
    public static final String EXPECTED = "tcm:12-14-16";
    public static final String EXPECTED_ISH = "ish:123-45-16";
    public static final String INCORRECT_URI = "incorrect:33-4f-zz";

    @Test
    public void testToString() throws ParseException {
        TCMURI tcmUri = new TCMURI(PUBLICATION_ID, ITEM_ID, ITEM_TYPE, VERSION);
        assertEquals(EXPECTED, tcmUri.toString());

        TCMURI tcmUriByString = new TCMURI(EXPECTED);
        assertEquals(EXPECTED, tcmUriByString.toString());
    }

    @Test(expected = ParseException.class)
    public void testCreateIncorrectTCMURI() throws ParseException {
        new TCMURI(INCORRECT_URI);
    }

    @Test
    public void testGetItemType() throws ParseException {
        TCMURI tcmUri = new TCMURI(PUBLICATION_ID, ITEM_ID, ITEM_TYPE, VERSION);
        assertEquals(ITEM_TYPE, tcmUri.getItemType());
    }

    @Test
    public void testGetItemId() throws ParseException {
        TCMURI tcmUri = new TCMURI(PUBLICATION_ID, ITEM_ID, ITEM_TYPE, VERSION);
        assertEquals(ITEM_ID, tcmUri.getItemId());
    }

    @Test
    public void testGetPublicationId() throws ParseException {
        TCMURI tcmUri = new TCMURI(PUBLICATION_ID, ITEM_ID, ITEM_TYPE, VERSION);
        assertEquals(PUBLICATION_ID, tcmUri.getPublicationId());
    }

    @Test
    public void testGetVersion() throws ParseException {
        TCMURI tcmUri = new TCMURI(PUBLICATION_ID, ITEM_ID, ITEM_TYPE, VERSION);
        assertEquals(VERSION, tcmUri.getVersion());
    }

    @Test
    public void testIsValid() {
        assertTrue(TCMURI.isValid(EXPECTED));
        assertTrue(TCMURI.isValid(EXPECTED_ISH));
        assertFalse(TCMURI.isValid(INCORRECT_URI));
    }
}