package io.katharsis.dispatcher.controller.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.katharsis.dispatcher.controller.BaseControllerTest;
import io.katharsis.queryParams.RequestParams;
import io.katharsis.request.dto.*;
import io.katharsis.request.path.JsonPath;
import io.katharsis.request.path.ResourcePath;
import io.katharsis.resource.exception.ResourceNotFoundException;
import io.katharsis.resource.mock.models.Project;
import io.katharsis.resource.mock.models.Task;
import io.katharsis.resource.mock.models.User;
import io.katharsis.resource.mock.repository.TaskToProjectRepository;
import io.katharsis.response.Container;
import io.katharsis.response.ResourceResponse;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourcePostTest extends BaseControllerTest {

    private static final String REQUEST_TYPE = "POST";

    @Test
    public void onGivenRequestCollectionGetShouldDenyIt() {
        // GIVEN
        JsonPath jsonPath = pathBuilder.buildPath("/tasks/1");
        ResourcePost sut = new ResourcePost(resourceRegistry, typeParser);

        // WHEN
        boolean result = sut.isAcceptable(jsonPath, REQUEST_TYPE);

        // THEN
        Assert.assertEquals(result, false);
    }

    @Test
    public void onGivenRequestResourceGetShouldAcceptIt() {
        // GIVEN
        JsonPath jsonPath = pathBuilder.buildPath("/tasks/");
        ResourcePost sut = new ResourcePost(resourceRegistry, typeParser);

        // WHEN
        boolean result = sut.isAcceptable(jsonPath, REQUEST_TYPE);

        // THEN
        Assert.assertEquals(result, true);
    }

    @Test
    public void onInconsistentResourceTypesShouldThrowException() throws Exception {
        // GIVEN
        RequestBody newProjectBody = new RequestBody();
        newProjectBody.setData(new DataBody());
        newProjectBody.getData().setType("projects");
        newProjectBody.getData().setAttributes(new Attributes());
        newProjectBody.getData().getAttributes().addAttribute("name", "sample project");

        JsonPath projectPath = pathBuilder.buildPath("/tasks");
        ResourcePost sut = new ResourcePost(resourceRegistry, typeParser);

        // THEN
        expectedException.expect(RuntimeException.class);

        // WHEN
        sut.handle(projectPath, new RequestParams(new ObjectMapper()), newProjectBody);
    }

    @Test
    public void onNonExistentResourceShouldThrowException() throws Exception {
        // GIVEN
        RequestBody newProjectBody = new RequestBody();
        newProjectBody.setData(new DataBody());
        newProjectBody.getData().setType("fridges");

        ResourcePost sut = new ResourcePost(resourceRegistry, typeParser);

        // THEN
        expectedException.expect(ResourceNotFoundException.class);

        // WHEN
        sut.handle(new ResourcePath("fridges"), new RequestParams(new ObjectMapper()), newProjectBody);
    }

    @Test
    public void onNoBodyResourceShouldThrowException() throws Exception {
        // GIVEN
        ResourcePost sut = new ResourcePost(resourceRegistry, typeParser);

        // THEN
        expectedException.expect(RuntimeException.class);

        // WHEN
        sut.handle(new ResourcePath("fridges"), new RequestParams(new ObjectMapper()), null);
    }

    @Test
    public void onNewResourcesAndRelationshipShouldPersistThoseData() throws Exception {
        // GIVEN
        RequestBody newProjectBody = new RequestBody();
        newProjectBody.setData(new DataBody());
        newProjectBody.getData().setType("projects");
        newProjectBody.getData().setAttributes(new Attributes());
        newProjectBody.getData().getAttributes().addAttribute("name", "sample project");

        JsonPath projectPath = pathBuilder.buildPath("/projects");
        ResourcePost sut = new ResourcePost(resourceRegistry, typeParser);

        // WHEN
        ResourceResponse projectResponse = sut.handle(projectPath, new RequestParams(new ObjectMapper()), newProjectBody);

        // THEN
        assertThat(projectResponse.getData()).isExactlyInstanceOf(Container.class);
        assertThat(((Container) projectResponse.getData()).getData()).isExactlyInstanceOf(Project.class);
        assertThat(((Project) (((Container) projectResponse.getData()).getData())).getId()).isNotNull();
        assertThat(((Project) (((Container) projectResponse.getData()).getData())).getName()).isEqualTo("sample project");
        Long projectId = ((Project) (((Container) projectResponse.getData()).getData())).getId();

        /* ------- */

        // GIVEN
        RequestBody newTaskBody = new RequestBody();
        newTaskBody.setData(new DataBody());
        newTaskBody.getData().setType("tasks");
        newTaskBody.getData().setAttributes(new Attributes());
        newTaskBody.getData().getAttributes().addAttribute("name", "sample task");
        newTaskBody.getData().setRelationships(new ResourceRelationships());
        newTaskBody.getData().getRelationships().setAdditionalProperty("project", new Linkage("projects", projectId.toString()));

        JsonPath taskPath = pathBuilder.buildPath("/tasks");

        // WHEN
        ResourceResponse taskResponse = sut.handle(taskPath, new RequestParams(new ObjectMapper()), newTaskBody);

        // THEN
        assertThat(taskResponse.getData()).isExactlyInstanceOf(Container.class);
        assertThat(((Container) taskResponse.getData()).getData()).isExactlyInstanceOf(Task.class);
        Long taskId = ((Task) (((Container) taskResponse.getData()).getData())).getId();
        assertThat(taskId).isNotNull();
        assertThat(((Task) (((Container) taskResponse.getData()).getData())).getName()).isEqualTo("sample task");

        TaskToProjectRepository taskToProjectRepository = new TaskToProjectRepository();
        Project project = taskToProjectRepository.findOneTarget(taskId, "project");
        assertThat(project.getId()).isEqualTo(projectId);
    }

    @Test
    public void onNewResourcesAndRelationshipsShouldPersistThoseData() throws Exception {
        // GIVEN
        RequestBody newProjectBody = new RequestBody();
        newProjectBody.setData(new DataBody());
        newProjectBody.getData().setType("projects");
        newProjectBody.getData().setAttributes(new Attributes());
        newProjectBody.getData().getAttributes().addAttribute("name", "sample project");

        JsonPath projectPath = pathBuilder.buildPath("/projects");
        ResourcePost sut = new ResourcePost(resourceRegistry, typeParser);

        // WHEN
        ResourceResponse projectResponse = sut.handle(projectPath, new RequestParams(new ObjectMapper()), newProjectBody);

        // THEN
        assertThat(projectResponse.getData()).isExactlyInstanceOf(Container.class);
        assertThat(((Container) projectResponse.getData()).getData()).isExactlyInstanceOf(Project.class);
        assertThat(((Project) (((Container) projectResponse.getData()).getData())).getId()).isNotNull();
        assertThat(((Project) (((Container) projectResponse.getData()).getData())).getName()).isEqualTo("sample project");
        Long projectId = ((Project) (((Container) projectResponse.getData()).getData())).getId();

        /* ------- */

        // GIVEN
        RequestBody newTaskBody = new RequestBody();
        newTaskBody.setData(new DataBody());
        newTaskBody.getData().setType("users");
        newTaskBody.getData().setAttributes(new Attributes());
        newTaskBody.getData().getAttributes().addAttribute("name", "some user");
        newTaskBody.getData().setRelationships(new ResourceRelationships());
        newTaskBody.getData().getRelationships().setAdditionalProperty("assignedProjects", Arrays.asList(new Linkage("projects",
                projectId.toString())));

        JsonPath taskPath = pathBuilder.buildPath("/users");

        // WHEN
        ResourceResponse taskResponse = sut.handle(taskPath, new RequestParams(new ObjectMapper()), newTaskBody);

        // THEN
        assertThat(taskResponse.getData()).isExactlyInstanceOf(Container.class);
        assertThat(((Container) taskResponse.getData()).getData()).isExactlyInstanceOf(User.class);
        Long userId = ((User) (((Container) taskResponse.getData()).getData())).getId();
        assertThat(userId).isNotNull();
        assertThat(((User) (((Container) taskResponse.getData()).getData())).getName()).isEqualTo("some user");

        assertThat(((User) (((Container) taskResponse.getData()).getData())).getAssignedProjects()).hasSize(1);
        assertThat(((User) (((Container) taskResponse.getData()).getData())).getAssignedProjects().get(0).getId()).isEqualTo(projectId);
    }
}
