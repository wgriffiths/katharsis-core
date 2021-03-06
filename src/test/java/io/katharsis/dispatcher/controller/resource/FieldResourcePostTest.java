package io.katharsis.dispatcher.controller.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.katharsis.dispatcher.controller.BaseControllerTest;
import io.katharsis.dispatcher.controller.HttpMethod;
import io.katharsis.queryParams.RequestParams;
import io.katharsis.request.dto.*;
import io.katharsis.request.path.JsonPath;
import io.katharsis.request.path.ResourcePath;
import io.katharsis.resource.exception.ResourceNotFoundException;
import io.katharsis.resource.mock.models.Project;
import io.katharsis.resource.mock.models.Task;
import io.katharsis.resource.mock.repository.TaskToProjectRepository;
import io.katharsis.resource.registry.ResourceRegistry;
import io.katharsis.response.BaseResponse;
import io.katharsis.response.Container;
import io.katharsis.response.ResourceResponse;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FieldResourcePostTest extends BaseControllerTest {
    private static final String REQUEST_TYPE = HttpMethod.POST.name();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void onValidRequestShouldAcceptIt() {
        // GIVEN
        JsonPath jsonPath = pathBuilder.buildPath("tasks/1/project");
        ResourceRegistry resourceRegistry = mock(ResourceRegistry.class);
        FieldResourcePost sut = new FieldResourcePost(resourceRegistry, typeParser);

        // WHEN
        boolean result = sut.isAcceptable(jsonPath, REQUEST_TYPE);

        // THEN
        assertThat(result).isTrue();
    }

    @Test
    public void onNonRelationRequestShouldDenyIt() {
        // GIVEN
        JsonPath jsonPath = new ResourcePath("tasks");
        ResourceRegistry resourceRegistry = mock(ResourceRegistry.class);
        FieldResourcePost sut = new FieldResourcePost(resourceRegistry, typeParser);

        // WHEN
        boolean result = sut.isAcceptable(jsonPath, REQUEST_TYPE);

        // THEN
        assertThat(result).isFalse();
    }

    @Test
    public void onNonExistentParentResourceShouldThrowException() throws Exception {
        // GIVEN
        RequestBody newProjectBody = new RequestBody();
        newProjectBody.setData(new DataBody());
        newProjectBody.getData().setType("projects");
        newProjectBody.getData().setAttributes(new Attributes());
        newProjectBody.getData().getAttributes().addAttribute("name", "sample project");

        JsonPath projectPath = pathBuilder.buildPath("/tasks/-1/project");
        FieldResourcePost sut = new FieldResourcePost(resourceRegistry, typeParser);

        // THEN
        expectedException.expect(ResourceNotFoundException.class);

        // WHEN
        sut.handle(projectPath, new RequestParams(OBJECT_MAPPER), newProjectBody);
    }

    @Test
    public void onExistingParentResourceShouldSaveIt() throws Exception {
        // GIVEN
        RequestBody newTaskBody = new RequestBody();
        newTaskBody.setData(new DataBody());
        newTaskBody.getData().setType("tasks");
        newTaskBody.getData().setAttributes(new Attributes());
        newTaskBody.getData().getAttributes().addAttribute("name", "sample task");
        newTaskBody.getData().setRelationships(new ResourceRelationships());

        JsonPath taskPath = pathBuilder.buildPath("/tasks");
        ResourcePost resourcePost = new ResourcePost(resourceRegistry, typeParser);

        // WHEN
        BaseResponse taskResponse = resourcePost.handle(taskPath, new RequestParams(new ObjectMapper()), newTaskBody);

        // THEN
        assertThat(taskResponse.getData()).isExactlyInstanceOf(Container.class);
        assertThat(((Container) taskResponse.getData()).getData()).isExactlyInstanceOf(Task.class);
        Long taskId = ((Task) (((Container) taskResponse.getData()).getData())).getId();
        assertThat(taskId).isNotNull();

        /* ------- */

        // GIVEN
        RequestBody newProjectBody = new RequestBody();
        newProjectBody.setData(new DataBody());
        newProjectBody.getData().setType("projects");
        newProjectBody.getData().setAttributes(new Attributes());
        newProjectBody.getData().getAttributes().addAttribute("name", "sample project");

        JsonPath projectPath = pathBuilder.buildPath("/tasks/" + taskId + "/project");
        FieldResourcePost sut = new FieldResourcePost(resourceRegistry, typeParser);

        // WHEN
        ResourceResponse projectResponse = sut.handle(projectPath, new RequestParams(OBJECT_MAPPER), newProjectBody);

        assertThat(projectResponse.getData()).isExactlyInstanceOf(Container.class);
        assertThat(((Container) projectResponse.getData()).getData()).isExactlyInstanceOf(Project.class);
        assertThat(((Project) (((Container) projectResponse.getData()).getData())).getId()).isNotNull();
        assertThat(((Project) (((Container) projectResponse.getData()).getData())).getName()).isEqualTo("sample project");
        Long projectId = ((Project) (((Container) projectResponse.getData()).getData())).getId();
        assertThat(projectId).isNotNull();

        TaskToProjectRepository taskToProjectRepository = new TaskToProjectRepository();
        Project project = taskToProjectRepository.findOneTarget(taskId, "project");
        assertThat(project.getId()).isEqualTo(projectId);
    }
}
