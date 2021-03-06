.. _schedule-tracking-module:

========================
Schedule Tracking Module
========================

.. contents::
   :depth: 2

Description
-----------

The schedule tracking module allows a client to be enrolled in a clearly defined schedule. Schedules consist of specified "milestones." Milestones represent windows of time in which certain criteria, treatments, courses, etc. should be fulfilled before moving on to the next milestone. Individuals enrolled in these schedules are sent alerts when they are due, late, or past due on the period of schedule fulfillment. Clients may be enrolled in an unlimited number of schedules. If a client is enrolled in a schedule they were already enrolled in, the service overwrites the previous enrollment with the new one.

Information for Implementation
------------------------------

A schedule defines a set of milestones that should be fulfilled in a specific window of time. Implementers can use the Schedule Tracking module to accomplish the following:

* Enroll a particular client in a schedule for a specific duration of time. This is accomplished by using the ScheduleTrackingService interface.
* Send alerts to clients that have been enrolled in a schedule.
* Define JSON documents that specify a schedule and its set of milestones.
* Query for client enrollments in schedules based on ID, schedule name, milestone name, milestone window, enrollment status, completion date, and metadata.

Client enrollments are stored in MOTECH Data Services (MDS).

Lifecycle of a Milestone
------------------------

* The first milestone is triggered at the start of the schedule. Subsequent milestones are started when the previous milestone is fulfilled.
* Once started, a milestone is in an *earliest*, *due*, *late*, or *max* window as per the schedule configuration. These window durations form the lifetime of the milestone.
* If a milestone is not fulfilled before the expiration of its lifetime, the associated client enrollment will be defaulted, and all of the previously configured alerts will not be raised.
* A milestone that is not fulfilled before the expiration of its lifetime is defaulted. You can also mark the milestone as defaulted within its lifetime.

Specifying a Schedule and its Milestones
----------------------------------------

Specifying the characteristics of a schedule and its milestones is typically accomplished using a JSON (JavaScript Object Notation) document. The **schedule_tracking.properties** file specifies the directory to scan for schedule definitions. Any JSON file in the directory will be treated as a schedule definition. Multiple schedules can be defined, each in their own file. Schedules have a name and a list of milestones. A milestone has:

* A name
* A list of four standard time windows (earliest, due, late, and max)
* A list of alerts
* A map of data (not currently used)

Window durations can be customized.

Sample Schedule Definition
--------------------------

For a sample schedule definition, including specifying window times, absolute window schedules, alerts,
and floating alerts, see :std:ref:`sample-schedule-definition`.

OSGi Service APIs
-----------------

The schedule tracking module exposes two OSGi interfaces, ScheduleTrackingService and EnrollmentActionService.

ScheduleTrackingService
^^^^^^^^^^^^^^^^^^^^^^^

ScheduleTrackingService enrolls and unenrolls users, updates enrollments, fulfills milestones, queries enrollments, and provides a preview of alert timings for a given enrollment.

.. code-block:: java

  public interface ScheduleTrackingService {

      String enroll(EnrollmentRequest enrollmentRequest);

      void fulfillCurrentMilestone(String externalId, String scheduleName,
          LocalDate fulfillmentDate, Time fulfillmentTime);

      void fulfillCurrentMilestone(String externalId, String scheduleName,
          LocalDate fulfillmentDate);

      void unenroll(String externalId, List<String> scheduleNames);

      EnrollmentRecord getEnrollment(String externalId, String scheduleName);

      void updateEnrollment(String externalId, String scheduleName,
          UpdateCriteria updateCriteria);

      List<EnrollmentRecord> search(EnrollmentsQuery query);

      List<EnrollmentRecord> searchWithWindowDates(EnrollmentsQuery query);

      MilestoneAlerts getAlertTimings(EnrollmentRequest enrollmentRequest);

      void add(String scheduleJson);

      Schedule getScheduleByName(String scheduleName);

      List<Schedule> getAllSchedules();

      void remove(String scheduleName);

  }

Enrolling a User in a Schedule
""""""""""""""""""""""""""""""

To enroll a user into a schedule, an EnrollmentRequest must be passed to the enroll() method in the ScheduleTrackingService. An EnrollmentRequest has the following information:

+-----------------------+-------------+------------------------------------+
| Parameter             | Type        | Description                        |
+=======================+=============+====================================+
| externalID            | String      | A unique ID for the client         |
+-----------------------+-------------+------------------------------------+
| scheduleName          | String      | The name of the schedule to enroll |
|                       |             | the user in; defined in the JSON   |
|                       |             | document                           |
+-----------------------+-------------+------------------------------------+
| startingMilestoneName | String      | The name of the first milestone    |
|                       |             | into which the user will be        |
|                       |             | directly enrolled                  |
+-----------------------+-------------+------------------------------------+
| referenceDate         | LocalDate   | The date on which the schedule     |
|                       |             | will start                         |
+-----------------------+-------------+------------------------------------+
| referenceTime         | Time        | (Optional; defaults to midnight)   |
|                       |             | Time, for fine-grained referencing |
+-----------------------+-------------+------------------------------------+
| enrollmentDate        | LocalDate   | The date on which the user is      |
|                       |             | enrolled into the schedule         |
+-----------------------+-------------+------------------------------------+
| enrollmentTime        | Time        | (Optional; defaults to midnight)   |
|                       |             | Time, for fine-grained referencing |
+-----------------------+-------------+------------------------------------+
| preferredAlertTime    | Time        | Time of day to send alerts to user |
+-----------------------+-------------+------------------------------------+
| metadata              | Map<String, | Additional information stored as   |
|                       |   String>   | property=>value pairs, e.g.,       |
|                       |             | facility_id=>1234                  |
+-----------------------+-------------+------------------------------------+

When the ScheduleTrackingService's enroll() method is invoked, the service determines whether that client is already enrolled and active in the schedule. If the client is already enrolled, the service overwrites the previous enrollment with the new one. A new enrollment record for the client is created and added to the database.

Fulfilling Milestones
"""""""""""""""""""""

The fulfillCurrentMilestone() method of the module's ScheduleTrackingService fulfills the current milestone of the client within an enrollment. After fulfillment of a milestone, the client moves to the next milestone in the schedule. If no more milestones remain the schedule, the enrollment is marked as complete. This fulfillment date and time is mandatory while fulfilling a milestone. The fulfillment date and time are used to make the fulfillment process idempotent. This ensures that invoking fulfillCurrentMilestone() more than once with the same fulfillment date and time will not make multiple fulfillments.

Defaulted Enrollments
"""""""""""""""""""""

For any milestone in an enrollment, if the milestone has not been fulfilled by the last day of the milestone, then that enrollment is marked as defaulted. The last day of the milestone is the day when all four windows of the milestone elapse. A defaulted enrollment will not raise any more alerts. It also cannot move to an active state, which is the default state of an enrollment that raises alerts.

Unenrolling a User from a Schedule
""""""""""""""""""""""""""""""""""

The unenroll() method of the module's ScheduleTrackingService removes a user from an active enrollment. Only active enrollments can be removed. Envoking unenroll() on an enrollment will cause the enrollment to be marked as *UNENROLLED* in the database. *DEFAULTED* and *COMPLETED* enrollments are also preserved in the database for record keeping.

Updating an Active Enrollment
"""""""""""""""""""""""""""""

The updateEnrollment() method of the module's ScheduleTrackingService updates an active enrollment. Currently, MOTECH supports updating only the metadata field of an active enrollment. Metadata property => value pair can be updated or inserted but cannot be deleted from an existing enrollment.

Example:

Consider an active enrollment with the following attributes:

  external id : "foo"
  schedule name : "some_schedule"
  metadata value pairs : {foo1: bar1; foo2: bar2}

::

  HashMap<String, String> toBeUpdatedMetadata = new HashMap<String, String>();

  toBeUpdatedMetadata.put("foo2", "val2");

  toBeUpdatedMetadata.put("foo3", "val3");

  UpdateCriteria updateCriteria = new UpdateCriteria().Metadata(toBeUpdatedMetadata);

  scheduleTrackingService.updateEnrollment("foo", "some_schedule", updateCriteria);

will update the metadata of the enrollment as {foo1:bar1; foo2: val2; foo3: val3}

Previewing Alert Timings
""""""""""""""""""""""""

The getAlertTimings() method of the module's ScheduleTrackingService provides a preview of alert timings given a particular enrollment request. For the given enrollment request, the alert timings of all windows of the current milestone will be returned. This gives an idea of the alerts that a client might miss if the enrollment were to be scheduled on the date specified by the enrollment request. This is useful in cases where the client needs to be enrolled into the middle of the milestone but should not miss any alerts. After previewing the alert timings, clients would be enrolled on a reference date that results in a schedule with no elapsed alerts.

Querying the API
""""""""""""""""

The search() method of the module's ScheduleTrackingService allows for querying enrollments. This allows clients to find enrollments using various criteria. Queries can be performed based on the following list of criteria:

* havingExternalId(externalId)
* havingSchedule(scheduleNames...)
* havingCurrentMilestone(milestoneName)
* havingWindowStartingDuring(WindowName, DateTime start, DateTime end)
* havingWindowEndingDuring(WindowName, DateTime start, DateTime end)
* currentlyInWindow(WindowNames...)
* havingState(EnrollmentStatus) (ACTIVE, DEFAULTED, COMPLETED, or UNENROLLED)
* completedDuring(DateTime start, DateTime end)
* havingMetadata(key, value)

Each of these methods returns an EnrollmentsQuery object, which the search() method takes as a parameter.

Examples:

.. code-block:: java

  scheduleTrackingService.search(new EnrollmentsQuery().havingState("active"))

will find all active enrollments.

.. code-block:: java

  scheduleTrackingService.search(

	    new EnrollmentsQuery()

		      .havingSchedule("IPTI Schedule")

		      .havingState("active")
          
		      .havingWindowStartingDuring(WindowName.due, weeksAgo(1), now))

will find active enrollments enrolled into the IPTI Schedule that will enter the due window any time in the next one week.

The return value of the search() method is a list of EnrollmentRecords. An EnrollmentRecord represents an enrollment in the system. EnrollmentRecords contain an external id, schedule name, preferred alert time, reference date and time, enrollment date and time, start dates for each of the four windows, and a reference to the current milestone.

.. code-block:: java

  public class EnrollmentRecord {

      private String externalId;

      private String scheduleName;

      private String currentMilestoneName;

      private DateTime referenceDateTime;

      private DateTime enrollmentDateTime;

      private Time preferredAlertTime;

      private DateTime earliestStart;

      private DateTime dueStart;

      private DateTime lateStart;

      private DateTime maxStart;

      private String status;

      private Map<String, String> metadata;

  }

EnrollmentActionService
^^^^^^^^^^^^^^^^^^^^^^^

EnrollmentActionService is a facade for ScheduleTrackingService that acts as a proxy for the Tasks module. Its two methods, enroll() and unenroll(), are exposed as task actions.

.. code-block:: java

  public interface EnrollmentActionService {

      void enroll(String externalId, String scheduleName, String preferredAlertTime, 
          DateTime referenceDate, String referenceTime, DateTime enrollmentDate, 
          String enrollmentTime, String startingMilestoneName);

      void unenroll(String externalId, String scheduleName);

  }

Events Consumed and Emitted
---------------------------

Consumed Event
^^^^^^^^^^^^^^

MILESTONE_DEFAULTED
"""""""""""""""""""

This module exposes an EndOfMilestoneListener, which handles and consumes events with the subject EventSubjects.MILESTONE_DEFAULTED.
::

  EventSubjects.MILESTONE_DEFAULTED (org.motechproject.scheduletracking.milestone.defaulted)

      Parameters/Payload:

	        EventDataKeys.ENROLLMENT_ID (enrollmentId)

	        EventDataKeys.EXTERNAL_ID (externalId)

	        MotechSchedulerService.JOB_ID_KEY (jobId)

Emitted Events
^^^^^^^^^^^^^^

MILESTONE_ALERT
"""""""""""""""

The EnrollmentAlertService emits events with the subject EventSubjects.MILESTONE_ALERT.
::

  EventSubjects.MILESTONE_ALERT (org.motechproject.scheduletracking.milestone.alert)

      Parameters/Payload:

    	    EventDataKeys.WINDOW_NAME (windowName)

     	    EventDataKeys.MILESTONE_NAME (milestoneAlert)

    	    EventDataKeys.SCHEDULE_NAME (scheduleName)

    	    EventDataKeys.EXTERNAL_ID (externalId)

    	    EventDataKeys.REFERENCE_DATE (referenceDateTime)

Milestone alert events are scheduled for the current milestone per each alert definition.

MILESTONE_DEFAULTED
"""""""""""""""""""

The EnrollmentDefaultmentService emits events with the subject EventSubjects.MILESTONE_DEFAULTED.
::

  EventSubjects.MILESTONE_DEFAULTED (org.motechproject.scheduletracking.milestone.defaulted)

      Parameters/Payload:

	        EventDataKeys.ENROLLMENT_ID (enrollmentId)

	        EventDataKeys.EXTERNAL_ID (externalId)

	        MotechSchedulerService.JOB_ID_KEY (jobId)

If the milestone has not been fulfilled by the last day of the milestone, then it is defaulted. Defaultment jobs are scheduled on the day a milestone would be defaulted. Their role is to capture and save the defaulted state of the milestone.

USER_ENROLLED
"""""""""""""

The EnrollmentService implementation emits events with the subject EventSubjects.USER_ENROLLED.
::

  EventSubjects.USER_ENROLLED (org.motechproject.scheduletracking.user.enrolled)

      Parameters/Payload:

	        EventDataKeys.EXTERNAL_ID (externalID)

	        EventDataKeys.SCHEDULE_NAME (scheduleName)

	        EventDataKeys.MILESTONE_NAME (startingMilestoneName)

	        EventDataKeys.PREFERRED_ALERT_TIME (preferredAlertTime)

	        EventDataKeys.REFERENCE_DATE (referenceDate)

	        EventDataKeys.REFERENCE_TIME (referenceTime)

	        EventDataKeys.ENROLLMENT_DATE (enrollmentDate)

	        EventDataKeys.ENROLLMENT_TIME (enrollmentTime)

USER_UNENROLLED
"""""""""""""""
The EnrollmentService implementation emits events with the subject EventSubjects.USER_UNENROLLED.
::

  EventSubjects.USER_UNENROLLED (org.motechproject.scheduletracking.user.unenrolled)

      Parameters/Payload:

	        EventDataKeys.EXTERNAL_ID (externalID)

	        EventDataKeys.SCHEDULE_NAME (scheduleName)

Roles and Permissions
---------------------

The Schedule Tracking module does not define any roles or permissions.
