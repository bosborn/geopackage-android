package mil.nga.geopackage;

import androidx.documentfile.provider.DocumentFile;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

import mil.nga.geopackage.io.ContextIOUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test GeoPackage Manager methods
 *
 * @author osbornb
 */
public class GeoPackageManagerTest extends BaseTestCase {

    /**
     * Import corrupt database name
     */
    private static final String IMPORT_CORRUPT_DB_NAME = "import_db_corrupt";

    /**
     * Import corrupt database file name, located in the test assets
     */
    private static final String IMPORT_CORRUPT_DB_FILE_NAME = IMPORT_CORRUPT_DB_NAME
            + "." + TestConstants.GEO_PACKAGE_EXTENSION;

    /**
     * Constructor
     */
    public GeoPackageManagerTest() {

    }

    @Before
    public void setUp() throws Exception {

        // Delete existing test databases
        GeoPackageManager manager = GeoPackageFactory.getManager(activity);
        manager.delete(TestConstants.TEST_DB_NAME);
        manager.delete(TestConstants.IMPORT_DB_NAME);
        manager.delete(IMPORT_CORRUPT_DB_NAME);
    }

    /**
     * Test creating and deleting a database
     */
    @Test
    public void testCreateOpenDelete() {

        GeoPackageManager manager = GeoPackageFactory.getManager(activity);

        // Verify does not exist
        assertFalse("Database already exists",
                manager.exists(TestConstants.TEST_DB_NAME));
        assertFalse("Database already returned in the set", manager
                .databaseSet().contains(TestConstants.TEST_DB_NAME));

        // Create
        assertTrue("Database failed to create",
                manager.create(TestConstants.TEST_DB_NAME));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.TEST_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.TEST_DB_NAME));

        // Open
        GeoPackage geoPackage = manager.open(TestConstants.TEST_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Open with inverse validation
        manager.setOpenHeaderValidation(!manager.isOpenHeaderValidation());
        manager.setOpenIntegrityValidation(!manager.isOpenIntegrityValidation());
        geoPackage = manager.open(TestConstants.TEST_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.TEST_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.TEST_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.TEST_DB_NAME));
    }

    /**
     * Test importing a database from a GeoPackage file
     */
    @Test
    public void testImport() {

        GeoPackageManager manager = GeoPackageFactory.getManager(activity);

        // Verify does not exist
        assertFalse("Database already exists",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database already returned in the set", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Copy the test db file from assets to the internal storage
        TestUtils.copyAssetFileToInternalStorage(activity, testContext,
                TestConstants.IMPORT_DB_FILE_NAME);

        // Import
        String importLocation = TestUtils.getAssetFileInternalStorageLocation(
                activity, TestConstants.IMPORT_DB_FILE_NAME);
        File importFile = new File(importLocation);
        assertTrue("Database not imported",
                manager.importGeoPackage(importFile));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validate(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateHeader(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateIntegrity(TestConstants.IMPORT_DB_NAME));

        // Open
        GeoPackage geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Attempt to import again
        try {
            manager.importGeoPackage(importFile);
            fail("Importing database again did not cause exception");
        } catch (GeoPackageException e) {
            // expected
        }

        // Import with override
        assertTrue("Database not imported",
                manager.importGeoPackage(importFile, true));

        // Open
        geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Try to import file with no extension
        String noFileExtension = TestUtils.getAssetFileInternalStorageLocation(
                activity, TestConstants.IMPORT_DB_NAME);
        try {
            manager.importGeoPackage(new File(noFileExtension));
            fail("GeoPackage without extension did not throw an exception");
        } catch (GeoPackageException e) {
            // Expected
        }

        // Try to import file with invalid extension
        String invalidFileExtension = TestUtils
                .getAssetFileInternalStorageLocation(activity,
                        TestConstants.IMPORT_DB_NAME + ".invalid");
        try {
            manager.importGeoPackage(new File(invalidFileExtension));
            fail("GeoPackage with invalid extension did not throw an exception");
        } catch (GeoPackageException e) {
            // Expected
        }

        // Try to import corrupt database
        TestUtils.copyAssetFileToInternalStorage(activity, testContext,
                IMPORT_CORRUPT_DB_FILE_NAME);
        String loadCorruptFileLocation = TestUtils
                .getAssetFileInternalStorageLocation(activity,
                        IMPORT_CORRUPT_DB_FILE_NAME);
        File loadCorruptFile = new File(loadCorruptFileLocation);
        try {
            manager.importGeoPackage(loadCorruptFile);
            fail("Corrupted GeoPackage did not throw an exception");
        } catch (GeoPackageException e) {
            // Expected
        }

        // Import and Open with inverse validation
        manager.setImportHeaderValidation(!manager.isImportHeaderValidation());
        manager.setImportIntegrityValidation(!manager.isImportIntegrityValidation());
        manager.setOpenHeaderValidation(!manager.isOpenHeaderValidation());
        manager.setOpenIntegrityValidation(!manager.isOpenIntegrityValidation());

        // Import
        assertTrue("Database not imported",
                manager.importGeoPackage(importFile));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validate(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateHeader(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateIntegrity(TestConstants.IMPORT_DB_NAME));

        // Open
        geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Delete the files
        assertTrue("Import file could not be deleted", importFile.delete());
        assertTrue("Corrupt Import file could not be deleted",
                loadCorruptFile.delete());
    }

    /**
     * Test importing a database from a GeoPackage document file
     */
    @Test
    public void testImportDocument() {

        GeoPackageManager manager = GeoPackageFactory.getManager(activity);

        // Verify does not exist
        assertFalse("Database already exists",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database already returned in the set", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Copy the test db file from assets to the internal storage
        TestUtils.copyAssetFileToInternalStorage(activity, testContext,
                TestConstants.IMPORT_DB_FILE_NAME);

        // Import
        String importLocation = TestUtils.getAssetFileInternalStorageLocation(
                activity, TestConstants.IMPORT_DB_FILE_NAME);
        File importFile = new File(importLocation);
        DocumentFile importDocument = DocumentFile.fromFile(importFile);
        assertTrue("Database not imported",
                manager.importGeoPackage(importDocument));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validate(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateHeader(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateIntegrity(TestConstants.IMPORT_DB_NAME));

        // Open
        GeoPackage geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Attempt to import again
        try {
            manager.importGeoPackage(importDocument);
            fail("Importing database again did not cause exception");
        } catch (GeoPackageException e) {
            // expected
        }

        // Import with override
        assertTrue("Database not imported",
                manager.importGeoPackage(importDocument, true));

        // Open
        geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Try to import file with no extension
        String noFileExtension = TestUtils.getAssetFileInternalStorageLocation(
                activity, TestConstants.IMPORT_DB_NAME);
        try {
            manager.importGeoPackage(new File(noFileExtension));
            fail("GeoPackage without extension did not throw an exception");
        } catch (GeoPackageException e) {
            // Expected
        }

        // Try to import file with invalid extension
        String invalidFileExtension = TestUtils
                .getAssetFileInternalStorageLocation(activity,
                        TestConstants.IMPORT_DB_NAME + ".invalid");
        try {
            manager.importGeoPackage(new File(invalidFileExtension));
            fail("GeoPackage with invalid extension did not throw an exception");
        } catch (GeoPackageException e) {
            // Expected
        }

        // Try to import corrupt database
        TestUtils.copyAssetFileToInternalStorage(activity, testContext,
                IMPORT_CORRUPT_DB_FILE_NAME);
        String loadCorruptFileLocation = TestUtils
                .getAssetFileInternalStorageLocation(activity,
                        IMPORT_CORRUPT_DB_FILE_NAME);
        File loadCorruptFile = new File(loadCorruptFileLocation);
        try {
            manager.importGeoPackage(loadCorruptFile);
            fail("Corrupted GeoPackage did not throw an exception");
        } catch (GeoPackageException e) {
            // Expected
        }

        // Import and Open with inverse validation
        manager.setImportHeaderValidation(!manager.isImportHeaderValidation());
        manager.setImportIntegrityValidation(!manager.isImportIntegrityValidation());
        manager.setOpenHeaderValidation(!manager.isOpenHeaderValidation());
        manager.setOpenIntegrityValidation(!manager.isOpenIntegrityValidation());

        // Import
        assertTrue("Database not imported",
                manager.importGeoPackage(importDocument));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validate(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateHeader(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateIntegrity(TestConstants.IMPORT_DB_NAME));

        // Open
        geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Delete the files
        assertTrue("Import file could not be deleted", importDocument.delete());
        assertTrue("Corrupt Import file could not be deleted",
                loadCorruptFile.delete());
    }

    /**
     * Test importing a database from a GeoPackage file
     *
     * @throws MalformedURLException
     */
    @Test
    public void testImportUrl() throws MalformedURLException {

        GeoPackageManager manager = GeoPackageFactory.getManager(activity);

        // Verify does not exist
        assertFalse("Database already exists",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database already returned in the set", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Import
        URL importUrl = new URL(TestConstants.IMPORT_URL);
        assertTrue("Database not imported", manager.importGeoPackage(
                TestConstants.IMPORT_DB_NAME, importUrl));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));

        // Open
        GeoPackage geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Import a fake url
        importUrl = new URL("http://doesnotexist/geopackage.gpkg");
        try {
            manager.importGeoPackage(TestConstants.IMPORT_DB_NAME, importUrl);
            fail("Successully imported a fake geopackage url");
        } catch (GeoPackageException e) {
            // expected
        }

        // Import and Open with inverse validation
        manager.setImportHeaderValidation(!manager.isImportHeaderValidation());
        manager.setImportIntegrityValidation(!manager.isImportIntegrityValidation());
        manager.setOpenHeaderValidation(!manager.isOpenHeaderValidation());
        manager.setOpenIntegrityValidation(!manager.isOpenIntegrityValidation());

        // Import
        importUrl = new URL(TestConstants.IMPORT_URL);
        assertTrue("Database not imported", manager.importGeoPackage(
                TestConstants.IMPORT_DB_NAME, importUrl));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));

        // Open
        geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));
    }

    /**
     * Test exporting a GeoPackage database to a file
     *
     * @throws SQLException
     */
    @Test
    public void testExport() throws SQLException {

        GeoPackageManager manager = GeoPackageFactory.getManager(activity);

        // Verify does not exist
        assertFalse("Database already exists",
                manager.exists(TestConstants.TEST_DB_NAME));
        assertFalse("Database already returned in the set", manager
                .databaseSet().contains(TestConstants.TEST_DB_NAME));

        // Create
        assertTrue("Database failed to create",
                manager.create(TestConstants.TEST_DB_NAME));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.TEST_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.TEST_DB_NAME));

        File exportDirectory = ContextIOUtils.getInternalFile(activity,
                null);

        // Delete previous exported file
        File exportedFile = new File(exportDirectory,
                TestConstants.TEST_DB_NAME + "."
                        + TestConstants.GEO_PACKAGE_EXTENSION);
        if (exportedFile.exists()) {
            assertTrue("Previous exported file could not be deleted",
                    exportedFile.delete());
        }
        assertFalse("Previous exported file still exists",
                exportedFile.exists());

        // Export the GeoPackage
        manager.exportGeoPackage(TestConstants.TEST_DB_NAME, exportDirectory);
        assertTrue("Exported file does not exist", exportedFile.exists());
        assertTrue("Exported file was empty", exportedFile.length() > 0);

        // Import
        TestUtils.copyAssetFileToInternalStorage(activity, testContext,
                TestConstants.IMPORT_DB_FILE_NAME);
        String importLocation = TestUtils.getAssetFileInternalStorageLocation(
                activity, TestConstants.IMPORT_DB_FILE_NAME);
        File importFile = new File(importLocation);
        assertTrue("Database not imported",
                manager.importGeoPackage(importFile));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));

        // Export the imported file
        String exportedImportName = "exportedImport";
        File exportedImport = new File(exportDirectory, exportedImportName
                + "." + TestConstants.GEO_PACKAGE_EXTENSION);
        manager.exportGeoPackage(TestConstants.IMPORT_DB_NAME,
                DocumentFile.fromFile(exportedImport));
        assertTrue("Exported import file does not exist",
                exportedImport.exists());
        assertTrue("Exported import file was empty",
                exportedImport.length() > 0);
        assertTrue(
                "Exported import file length is smaller than the original import file size",
                importFile.length() <= exportedImport.length());

        // Import the exported again, using override
        assertTrue("Database not imported", manager.importGeoPackage(
                TestConstants.IMPORT_DB_NAME, exportedImport, true));

        // Open and query
        GeoPackage geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        assertTrue(
                "Failed to query from imported exported database",
                geoPackage.getSpatialReferenceSystemDao().queryForAll().size() > 0);
        geoPackage.close();

        // Delete the test database
        assertTrue("Database not deleted",
                manager.delete(TestConstants.TEST_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.TEST_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.TEST_DB_NAME));

        // Delete the import database
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Delete the files
        assertTrue("Exported file could not be deleted", exportedFile.delete());
        assertTrue("Exported import file could not be deleted",
                exportedImport.delete());
        assertTrue("Import file could not be deleted", importFile.delete());
    }

    /**
     * Test importing a database from a GeoPackage file as an external link
     */
    @Test
    public void testImportAsExternalLink() {

        GeoPackageManager manager = GeoPackageFactory.getManager(activity);

        // Verify does not exist
        assertFalse("Database already exists",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database already returned in the set", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Copy the test db file from assets to the internal storage
        TestUtils.copyAssetFileToInternalStorage(activity, testContext,
                TestConstants.IMPORT_DB_FILE_NAME);

        // Import
        String importLocation = TestUtils.getAssetFileInternalStorageLocation(
                activity, TestConstants.IMPORT_DB_FILE_NAME);
        File importFile = new File(importLocation);
        assertTrue("Database not imported",
                manager.importGeoPackageAsExternalLink(importLocation));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));
        assertTrue("External file does not exist as database",
                manager.existsAtExternalFile(importFile));
        assertEquals("External files do not match",
                importFile, manager.getFile(TestConstants.IMPORT_DB_NAME));
        assertEquals("Database name does not match",
                TestConstants.IMPORT_DB_NAME, manager.getDatabaseAtExternalFile(importFile));
        assertTrue(manager.validate(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateHeader(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateIntegrity(TestConstants.IMPORT_DB_NAME));

        // Open
        GeoPackage geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Attempt to import again
        try {
            manager.importGeoPackageAsExternalLink(importFile, TestConstants.IMPORT_DB_NAME);
            fail("Importing database again did not cause exception");
        } catch (GeoPackageException e) {
            // expected
        }

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));
        assertFalse("External file exists as database after delete",
                manager.existsAtExternalFile(importFile));

        // Try to import corrupt database
        TestUtils.copyAssetFileToInternalStorage(activity, testContext,
                IMPORT_CORRUPT_DB_FILE_NAME);
        String loadCorruptFileLocation = TestUtils
                .getAssetFileInternalStorageLocation(activity,
                        IMPORT_CORRUPT_DB_FILE_NAME);
        File loadCorruptFile = new File(loadCorruptFileLocation);
        try {
            manager.importGeoPackageAsExternalLink(loadCorruptFile, TestConstants.IMPORT_DB_NAME);
            fail("Corrupted GeoPackage did not throw an exception");
        } catch (GeoPackageException e) {
            // Expected
        }

        // Import and Open with inverse validation
        manager.setImportHeaderValidation(!manager.isImportHeaderValidation());
        manager.setImportIntegrityValidation(!manager.isImportIntegrityValidation());
        manager.setOpenHeaderValidation(!manager.isOpenHeaderValidation());
        manager.setOpenIntegrityValidation(!manager.isOpenIntegrityValidation());

        // Import
        assertTrue("Database not imported",
                manager.importGeoPackageAsExternalLink(importFile, TestConstants.IMPORT_DB_NAME));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));
        assertTrue("External file does not exist as database",
                manager.existsAtExternalFile(importFile));
        assertTrue(manager.validate(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateHeader(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateIntegrity(TestConstants.IMPORT_DB_NAME));

        // Open
        geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));
        assertFalse("External file exists as database after delete",
                manager.existsAtExternalFile(importFile));

        // Delete the files
        assertTrue("Import file could not be deleted", importFile.delete());
        assertTrue("Corrupt Import file could not be deleted",
                loadCorruptFile.delete());
    }

    /**
     * Test importing a database from a GeoPackage document file as an external link
     */
    @Test
    public void testImportDocumentAsExternalLink() {

        GeoPackageManager manager = GeoPackageFactory.getManager(activity);

        // Verify does not exist
        assertFalse("Database already exists",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database already returned in the set", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Copy the test db file from assets to the internal storage
        TestUtils.copyAssetFileToInternalStorage(activity, testContext,
                TestConstants.IMPORT_DB_FILE_NAME);

        // Import
        String importLocation = TestUtils.getAssetFileInternalStorageLocation(
                activity, TestConstants.IMPORT_DB_FILE_NAME);
        File importFile = new File(importLocation);
        DocumentFile importDocument = DocumentFile.fromFile(importFile);
        assertTrue("Database not imported",
                manager.importGeoPackageAsExternalLink(importDocument));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));
        assertTrue("External file does not exist as database",
                manager.existsAtExternalFile(importDocument));
        assertEquals("External files do not match",
                importDocument.getUri(), manager.getDocumentFile(TestConstants.IMPORT_DB_NAME).getUri());
        assertEquals("Database name does not match",
                TestConstants.IMPORT_DB_NAME, manager.getDatabaseAtExternalFile(importDocument));
        assertTrue(manager.validate(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateHeader(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateIntegrity(TestConstants.IMPORT_DB_NAME));

        // Open
        GeoPackage geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Attempt to import again
        try {
            manager.importGeoPackageAsExternalLink(importDocument, TestConstants.IMPORT_DB_NAME);
            fail("Importing database again did not cause exception");
        } catch (GeoPackageException e) {
            // expected
        }

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));
        assertFalse("External file exists as database after delete",
                manager.existsAtExternalFile(importDocument));

        // Try to import corrupt database
        TestUtils.copyAssetFileToInternalStorage(activity, testContext,
                IMPORT_CORRUPT_DB_FILE_NAME);
        String loadCorruptFileLocation = TestUtils
                .getAssetFileInternalStorageLocation(activity,
                        IMPORT_CORRUPT_DB_FILE_NAME);
        File loadCorruptFile = new File(loadCorruptFileLocation);
        try {
            manager.importGeoPackageAsExternalLink(loadCorruptFile, TestConstants.IMPORT_DB_NAME);
            fail("Corrupted GeoPackage did not throw an exception");
        } catch (GeoPackageException e) {
            // Expected
        }

        // Import and Open with inverse validation
        manager.setImportHeaderValidation(!manager.isImportHeaderValidation());
        manager.setImportIntegrityValidation(!manager.isImportIntegrityValidation());
        manager.setOpenHeaderValidation(!manager.isOpenHeaderValidation());
        manager.setOpenIntegrityValidation(!manager.isOpenIntegrityValidation());

        // Import
        assertTrue("Database not imported",
                manager.importGeoPackageAsExternalLink(importDocument));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));
        assertTrue("External file does not exist as database",
                manager.existsAtExternalFile(importDocument));
        assertTrue(manager.validate(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateHeader(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateIntegrity(TestConstants.IMPORT_DB_NAME));

        // Open
        geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));
        assertFalse("External file exists as database after delete",
                manager.existsAtExternalFile(importDocument));

        // Delete the files
        assertTrue("Import file could not be deleted", importDocument.delete());
        assertTrue("Corrupt Import file could not be deleted",
                loadCorruptFile.delete());
    }

    /**
     * Test opening an external file GeoPackage
     */
    @Test
    public void testOpenExternal() {

        GeoPackageManager manager = GeoPackageFactory.getExternalManager();

        // Copy the test db file from assets to the internal storage
        TestUtils.copyAssetFileToInternalStorage(activity, testContext,
                TestConstants.IMPORT_DB_FILE_NAME);

        // Open
        String externalLocation = TestUtils.getAssetFileInternalStorageLocation(
                activity, TestConstants.IMPORT_DB_FILE_NAME);
        File externalFile = new File(externalLocation);
        DocumentFile externalDocument = DocumentFile.fromFile(externalFile);

        GeoPackage geoPackage = manager.openExternal(externalLocation);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        geoPackage = manager.openExternal(externalFile);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        geoPackage = manager.openExternal(externalDocument);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        geoPackage = GeoPackageFactory.openExternal(externalLocation);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        geoPackage = GeoPackageFactory.openExternal(externalFile);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Delete the file
        assertTrue("External file could not be deleted", externalFile.delete());
    }

    /**
     * Test creating a database GeoPackage at a file
     */
    @Test
    public void testCreateFile() {
        createExternal(0);
    }

    /**
     * Test creating a database GeoPackage at a file with specified name
     */
    @Test
    public void testCreateFileWithName() {
        createExternal(1);
    }

    /**
     * Test creating a database GeoPackage at a path with specified name
     */
    @Test
    public void testCreateAtPath() {
        createExternal(2);
    }

    /**
     * Test creating a database GeoPackage at a file
     */
    @Test
    public void testCreateDocument() {
        createExternal(3);
    }

    /**
     * Test creating a database GeoPackage at a file with specified name
     */
    @Test
    public void testCreateDocumentWithName() {
        createExternal(4);
    }

    /**
     * Test creating a database GeoPackage file as an external link
     */
    private void createExternal(int testCase) {

        GeoPackageManager manager = GeoPackageFactory.getManager(activity);

        // Verify does not exist
        assertFalse("Database already exists",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database already returned in the set", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Create
        File externalDirectory = activity.getExternalCacheDir();
        File createFile = new File(externalDirectory, TestConstants.IMPORT_DB_FILE_NAME);
        createFile.delete();
        createExternal(manager, externalDirectory, createFile, testCase);
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validate(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateHeader(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateIntegrity(TestConstants.IMPORT_DB_NAME));

        // Open
        GeoPackage geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Attempt to create again
        try {
            createExternal(manager, externalDirectory, createFile, testCase);
            fail("Creating database again did not cause exception");
        } catch (GeoPackageException e) {
            // expected
        }

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));
        assertTrue("Create file could not be deleted", createFile.delete());

        // Import and Open with inverse validation
        manager.setImportHeaderValidation(!manager.isImportHeaderValidation());
        manager.setImportIntegrityValidation(!manager.isImportIntegrityValidation());
        manager.setOpenHeaderValidation(!manager.isOpenHeaderValidation());
        manager.setOpenIntegrityValidation(!manager.isOpenIntegrityValidation());

        // Create
        createExternal(manager, externalDirectory, createFile, testCase);
        assertTrue("Database does not exist",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertTrue("Database not returned in the set", manager.databaseSet()
                .contains(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validate(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateHeader(TestConstants.IMPORT_DB_NAME));
        assertTrue(manager.validateIntegrity(TestConstants.IMPORT_DB_NAME));

        // Open
        geoPackage = manager.open(TestConstants.IMPORT_DB_NAME);
        assertNotNull("Failed to open database", geoPackage);
        geoPackage.close();

        // Delete
        assertTrue("Database not deleted",
                manager.delete(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database exists after delete",
                manager.exists(TestConstants.IMPORT_DB_NAME));
        assertFalse("Database returned in the set after delete", manager
                .databaseSet().contains(TestConstants.IMPORT_DB_NAME));

        // Delete the file
        assertTrue("Create file could not be deleted", createFile.delete());

    }

    /**
     * Create the externally linked GeoPackage
     *
     * @param manager
     * @param externalDirectory
     * @param createFile
     * @param testCase
     */
    private void createExternal(GeoPackageManager manager, File externalDirectory, File createFile, int testCase) {
        boolean created = false;
        switch (testCase) {
            case 0:
                created = manager.createFile(createFile);
                break;
            case 1:
                created = manager.createFile(TestConstants.IMPORT_DB_NAME, createFile);
                break;
            case 2:
                created = manager.createAtPath(TestConstants.IMPORT_DB_NAME, externalDirectory);
                break;
            case 3:
                created = manager.createFile(DocumentFile.fromFile(createFile));
                break;
            case 4:
                created = manager.createFile(TestConstants.IMPORT_DB_NAME, DocumentFile.fromFile(createFile));
                break;
            default:
                fail("Unsupported test case");
        }
        assertTrue("Database not created",
                created);
    }

    /**
     * Test the memory footprint when repeatedly opening and closing a
     * GeoPackage
     */
    @Test
    public void memoryTest() {

        GeoPackageManager manager = GeoPackageFactory.getManager(activity);

        // Create
        assertTrue("Database failed to create",
                manager.create(TestConstants.TEST_DB_NAME));
        assertTrue("Database does not exist",
                manager.exists(TestConstants.TEST_DB_NAME));

        // Try to garbage collect
        System.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        // Track current memory usage
        long initialMemory = Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory();

        for (int i = 0; i < 1000; i++) {
            GeoPackage geoPackage = manager.open(TestConstants.TEST_DB_NAME);
            geoPackage.close();
        }

        // Try to garbage collect
        System.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        long currentMemory = Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory();

        // Less than 10 times initial memory
        assertTrue(currentMemory <= 10 * initialMemory);
    }

}
