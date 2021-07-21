package org.smartregister.reveal.model;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.dao.AbstractDao;
import org.smartregister.reveal.contract.ChildFilterFragmentContract;
import org.smartregister.reveal.util.Constants;
import org.smartregister.util.QueryComposer;

import java.util.List;

public class ChildFilterFragmentModel extends AbstractDao implements ChildFilterFragmentContract.Model {

    @Override
    public List<String> fetchUniqueGrades(String schoolID) throws QueryComposer.InvalidQueryException {
        QueryComposer composer = new QueryComposer()
                .withColumn("distinct " + Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.GRADE)
                .withMainTable(Constants.DatabaseKeys.CHILD_TABLE)
                .withSortColumn(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.GRADE)
                .withWhereClause(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.GRADE + " is not null");

        if (StringUtils.isNotBlank(schoolID))
            composer.withWhereClause(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.LOCATION + " = '" + schoolID + "'");

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, Constants.DatabaseKeys.GRADE);

        return AbstractDao.readData(composer.generateQuery(), dataMap);
    }
}
