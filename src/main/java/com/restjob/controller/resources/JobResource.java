/*
 * This file is part of RESTjob Controller.
 *
 * RESTjob Controller is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * RESTjob Controller is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * RESTjob Controller. If not, see http://www.gnu.org/licenses/.
 */
package com.restjob.controller.resources;

import com.restjob.controller.listener.LocalEntityManagerFactory;
import com.restjob.controller.model.Job;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/job")
public class JobResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllJobs() {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        List result = em.createNamedQuery("Job.getJobs").getResultList();
        em.close();
        return Response.ok(result).build();
    }

    @GET
    @Path("{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobByUuid(@PathParam("uuid") String uuid) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        TypedQuery<Job> query = em.createNamedQuery("Job.getJobByUuid", Job.class).setParameter("uuid", uuid);
        List<Job> jobs = query.getResultList();
        em.close();
        if (jobs.size() == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            Job job = jobs.get(0);
            return Response.ok(job).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addJob(MultivaluedMap<String, String> formParams) {
        if (formParams == null || (!(formParams.containsKey("payload") && formParams.containsKey("provider")))) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        List<Job> jobs = new ArrayList<>();
        for (String payload: formParams.get("payload")) {
            EntityManager em = LocalEntityManagerFactory.createEntityManager();
            em.getTransaction().begin();
            Job job = new Job();
            job.setProvider(formParams.getFirst("provider"));
            job.setPayload(payload);
            em.persist(job);
            em.getTransaction().commit();
            em.close();
            jobs.add(job);
        }
        return Response.ok(jobs).build();
    }

}
