package edu.oregonstate.mist.personswrite

import com.codahale.metrics.annotation.Timed
import edu.oregonstate.mist.api.Error
import edu.oregonstate.mist.api.Resource
import edu.oregonstate.mist.api.jsonapi.ResourceObject
import edu.oregonstate.mist.api.jsonapi.ResultObject
import edu.oregonstate.mist.contrib.JobObject
import edu.oregonstate.mist.contrib.PersonObjectException
import groovy.transform.TypeChecked

import javax.annotation.security.PermitAll
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("persons")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@TypeChecked
class PersonsWriteResource extends Resource {
    private final PersonsWriteDAO personsWriteDAO
    
    PersonsWriteResource(PersonsWriteDAO personsWriteDAO) {
        this.personsWriteDAO = personsWriteDAO
    }

    @Timed
    @POST
    @Consumes (MediaType.APPLICATION_JSON)
    @Path('{osuID: [0-9]+}/jobs')
    Response createJob(@PathParam('osuID') String osuID,
                       @Valid ResultObject resultObject) {
        if (!personsWriteDAO.personExist(osuID)) {
            return notFound().build()
        }

        List<Error> errors = newJobErrors(resultObject)

        if (errors) {
            Response.ResponseBuilder responseBuilder = Response.status(Response.Status.BAD_REQUEST)
            return responseBuilder.entity(errors).build()
        }

        JobObject job = JobObject.fromResultObject(resultObject)

        String createJobResult = personsWriteDAO.createJob(osuID, job).getString("return_value")

        //TODO: Should we be checking other conditions besides an null/empty string?
        // null string means success, I guess?
        if (!createJobResult) {
            accepted(new ResultObject(data: new ResourceObject(attributes: job))).build()
        } else {
            internalServerError("Error creating new job: $createJobResult").build()
        }
    }

    private List<Error> newJobErrors(ResultObject resultObject) {
        List<Error> errors = []

        JobObject job

        def addBadRequest = { String message ->
            errors.add(Error.badRequest(message))
        }

        try {
            job = JobObject.fromResultObject(resultObject)
        } catch (PersonObjectException e) {
            addBadRequest("Could not parse job object. " +
                    "Make sure dates are in ISO8601 format: yyyy-MM-dd")

            // if we can't deserialize the job object, no need to proceed
            return errors
        }

        if (!job) {
            addBadRequest("No job object provided.")

            // if there's no job object, no need to proceed
            return errors
        }

        // at this point, we have a job object. Let's validate the fields
        def requiredFields = ["Position number": job.positionNumber,
                              "Begin date": job.beginDate,
                              "Supervisor OSU ID": job.supervisorOsuID,
                              "Supervisor position number": job.supervisorPositionNumber]

        requiredFields.findAll { key, value -> !value }.each { key, value ->
            addBadRequest("${key} is required.")
        }

        def positiveNumberFields = ["Hourly rate": job.hourlyRate,
                                    "Hours per pay": job.hoursPerPay,
                                    "Assignment salary": job.assignmentSalary,
                                    "Annual salary": job.annualSalary,
                                    "Pays per year": job.paysPerYear]

        positiveNumberFields.findAll { key, value ->
            value && value < 0
        }.each { key, value ->
            addBadRequest("${key} cannot be a negative number.")
        }

        if (job.status && !job.isActive()) {
            addBadRequest("'Active' is the only valid job status.")
        }

        if (job.beginDate && job.endDate && (job.beginDate >= job.endDate)) {
            addBadRequest("End date must be after begin date.")
        }

        if (job.fullTimeEquivalency &&
                (job.fullTimeEquivalency > 1 || job.fullTimeEquivalency <= 0)) {
            addBadRequest("Full time equivalency must range from 0 to 1.")
        }

        if (job.appointmentPercent &&
                (job.appointmentPercent > 100 || job.appointmentPercent < 0)) {
            addBadRequest("Appointment percent must range from 0 to 100.")
        }

        Boolean validSupervisor = job.supervisorOsuID &&
                personsWriteDAO.personExist(job.supervisorOsuID)

        if (!validSupervisor) {
            addBadRequest("Supervisor OSU ID does not exist.")
        }

        Boolean validSupervisorPosition = personsWriteDAO.isActivePosition(
                job.supervisorOsuID, job.supervisorPositionNumber)

        if (validSupervisor
                && !validSupervisorPosition) {
            addBadRequest("Supervisor does not have an actjve position with position number " +
                    "${job.supervisorPositionNumber}")
        }

        if (job.positionNumber && !personsWriteDAO.isValidPositionNumber(job.positionNumber)) {
            addBadRequest("${job.positionNumber} is not a valid position number.")
        }

        if (job.locationID && !personsWriteDAO.isValidLocation(job.locationID)) {
            addBadRequest("${job.locationID} is not a valid location ID.")
        }

        if (job.timesheetOrganizationCode && !personsWriteDAO.isValidOrganizationCode(
                job.timesheetOrganizationCode)) {
            addBadRequest("${job.timesheetOrganizationCode} is not a valid organization code.")
        }

        if (job.laborDistribution) {
            BigDecimal totalDistributionPercent = 0

            job.laborDistribution.each {
                if (it.distributionPercent) {
                    totalDistributionPercent += it.distributionPercent
                } else {
                    addBadRequest("distributionPercent is required for each labor distribution.")
                }

                if (!it.accountIndexCode) {
                    addBadRequest("accountIndexCode is required for each labor distribution")
                } else if (!personsWriteDAO.isValidAccountIndexCode(it.accountIndexCode)) {
                    addBadRequest("${it.accountIndexCode} is not a valid accountIndexCode.")
                }

                if (it.accountCode && !personsWriteDAO.isValidAccountCode(it.accountCode)) {
                    addBadRequest("${it.accountCode} is not a valid accountCode.")
                }

                if (it.activityCode && !personsWriteDAO.isValidActivityCode(it.activityCode)) {
                    addBadRequest("${it.activityCode} is not a valid activityCode.")
                }
            }

            if (totalDistributionPercent != 100) {
                addBadRequest("Total sum of labor distribution percentages must equal 100.")
            }
        }

        errors
    }
}
