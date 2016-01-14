package org.motechproject.dhis2.rest.domain.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.motechproject.dhis2.rest.domain.AttributeDto;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseEnrollmentDto {

    private String trackedEntityInstance;
    private String program;
    private List<AttributeDto> attributes;
    private boolean followup;

    public BaseEnrollmentDto() {
    }

    public BaseEnrollmentDto(String trackedEntityInstance, String program, List<AttributeDto> attributes, boolean followup) {
        this.trackedEntityInstance = trackedEntityInstance;
        this.program = program;
        this.attributes = attributes;
        this.followup = followup;
    }

    public String getTrackedEntityInstance() {
        return trackedEntityInstance;
    }

    public void setTrackedEntityInstance(String trackedEntityInstance) {
        this.trackedEntityInstance = trackedEntityInstance;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public List<AttributeDto> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeDto> attributes) {
        this.attributes = attributes;
    }

    public boolean isFollowup() {
        return followup;
    }

    public void setFollowup(boolean followup) {
        this.followup = followup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEnrollmentDto)) {
            return false;
        }
        BaseEnrollmentDto that = (BaseEnrollmentDto) o;
        return followup == that.followup &&
                Objects.equals(trackedEntityInstance, that.trackedEntityInstance) &&
                Objects.equals(program, that.program) &&
                Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackedEntityInstance, program, attributes, followup);
    }
}
