package edu.oregonstate.mist.personswrite

import edu.oregonstate.mist.contrib.AbstractPersonsDAO
import edu.oregonstate.mist.contrib.JobObject
import org.skife.jdbi.v2.OutParameters
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.BindBean
import org.skife.jdbi.v2.sqlobject.SqlCall
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.customizers.OutParameter

import java.sql.Types

public interface PersonsWriteDAO extends Closeable {
    @SqlQuery(AbstractPersonsDAO.personExist)
    String personExist(@Bind('osuID') String osuID)

    @SqlQuery(AbstractPersonsDAO.validatePositionIsActive)
    Boolean isActivePosition(@Bind('osuID') String osuID,
                             @Bind('positionNumber') String positionNumber)

    @SqlQuery(AbstractPersonsDAO.validatePositionNumber)
    Boolean isValidPositionNumber(@Bind('positionNumber') String positionNumber)

    @SqlQuery(AbstractPersonsDAO.validateLocationID)
    Boolean isValidLocation(@Bind('locationID') String locationID)

    @SqlQuery(AbstractPersonsDAO.validateAccountIndexCode)
    Boolean isValidAccountIndexCode(@Bind('accountIndexCode') String accountIndexCode)

    @SqlQuery(AbstractPersonsDAO.validateAccountCode)
    Boolean isValidAccountCode(@Bind('accountCode') String accountCode)

    @SqlQuery(AbstractPersonsDAO.validateActivityCode)
    Boolean isValidActivityCode(@Bind('activityCode') String activityCode)

    @SqlQuery(AbstractPersonsDAO.validateOrganizationCode)
    Boolean isValidOrganizationCode(@Bind('organizationCode') String organizationCode)

    @SqlCall(AbstractPersonsDAO.createJobFunction)
    @OutParameter(name = "return_value", sqlType = Types.VARCHAR)
    OutParameters createJob(@Bind('osuID') String osuID,
                            @BindBean("job") JobObject job)
}
