package org.smartregister.reveal.dao;

import net.sqlcipher.MatrixCursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.domain.Location;
import org.smartregister.repository.Repository;

import java.util.List;
import java.util.Map;

public class StructureDaoTest extends StructureDao {

    @Mock
    private Repository repository;

    @Mock
    private SQLiteDatabase database;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        setRepository(repository);
        Mockito.doReturn(database).when(repository).getReadableDatabase();
    }

    @Test
    public void testGetStructuresByParentExecuteOnDB() {
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"name", "geojson"});
        matrixCursor.addRow(new Object[]{"sample", "{}"});
        matrixCursor.addRow(new Object[]{"sample", "{}"});
        matrixCursor.addRow(new Object[]{"sample1", "{}"});

        Mockito.doReturn(matrixCursor).when(database).rawQuery(Mockito.any(), Mockito.any());


        Map<String, List<Location>> result = StructureDao.getStructuresByParent();
        Assert.assertEquals(result.size(), 2);
        Assert.assertEquals(result.get("sample").size(), 2);
        Assert.assertEquals(result.get("sample1").size(), 1);
        Mockito.verify(database).rawQuery(stringArgumentCaptor.capture(), Mockito.any());

        String expected = "select l.name , s.geojson from structure s inner join location l on s.parent_id = l._id";
        Assert.assertEquals(expected, stringArgumentCaptor.getValue().toLowerCase().trim());
    }

    @Test
    public void testGetCachedStructures() {
        cache = Mockito.spy(cache);
        StructureDao.getCachedStructures();
        Mockito.verify(cache).get(Mockito.eq(CACHED_STRUCTURES), Mockito.any());
    }

    @Test
    public void testResetLocationCache() {
        cache = Mockito.spy(cache);
        StructureDao.resetLocationCache();
        Mockito.verify(cache).evict(CACHED_STRUCTURES);
    }
}
