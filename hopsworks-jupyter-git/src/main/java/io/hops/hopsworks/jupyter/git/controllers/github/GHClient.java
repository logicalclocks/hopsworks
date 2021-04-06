/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jupyter.git.controllers.github;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jupyter.git.controllers.RemoteGitClient;
import io.hops.hopsworks.jupyter.git.controllers.qualifiers.GitHub;
import io.hops.hopsworks.restutils.RESTCodes;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.GitHubResponse;
import org.eclipse.egit.github.core.service.OAuthService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@GitHub
public class GHClient implements RemoteGitClient {
  private static final Logger LOG = Logger.getLogger(GHClient.class.getName());
  
  private static final Pattern REPO_ATTRS = Pattern.compile("http(s)?://(?<type>.+)/(?<organization>.+)/(?<repository>"
    + ".+)\\.git");
  @EJB
  private GHClientCache clientCache;
  
  /**
   * Fetches remote branches names from GitHub. First branch is always the default.
   *
   * @param apiKey API key to communicate with GitHub
   * @param repository Repository name
   * @return Set of remote branches. First branch name in the set is the configured default branch of the repository
   * @throws ServiceException
   * @throws IOException
   */
  @Override
  public Set<String> fetchBranches(SecretPlaintext apiKey, String repository) throws ServiceException, IOException {
    GitHubClient client = clientCache.getClient(getHost(repository), apiKey);
    RepositoryService repositoryService = getRepositoryService(client);
    Repository repo = getRepository(repository, repositoryService);
    List<RepositoryBranch> branches = getBranches(repo, repositoryService);
    Set<String> flatBranches = new LinkedHashSet<>(branches.size());
    flatBranches.add(repo.getMasterBranch());
    branches.stream().map(RepositoryBranch::getName).forEach(flatBranches::add);
    return flatBranches;
  }

  @Override
  public boolean hasWriteAccess(SecretPlaintext apiKey, String repository) throws ServiceException, IOException {
    GitHubClient client = clientCache.getClient(getHost(repository), apiKey);
    GitHubRequest request = new GitHubRequest();
    request.setUri("/repos/" + getOrganizationName(repository) + "/" + getRepositoryName(repository));
    GitHubResponse response = client.get(request);
    //Check response header for token permissions https://docs.github.com/en/developers/apps/scopes-for-oauth-apps
    //Permissions are in a comma-separated list: "repo, write:packages"

    String scopes = response.getHeader("X-OAuth-Scopes");
    if(Strings.isNullOrEmpty(scopes)) {
      return false;
    } else {
      RepositoryService repositoryService = getRepositoryService(client);
      List<String> scopesList = new ArrayList<>(Arrays.asList(scopes.split(",")));
      scopesList = scopesList.stream().map(String::trim).collect(Collectors.toList());
      Repository repo = getRepositoryForOauthUser(getOrganizationName(repository),
        getRepositoryName(repository), repositoryService);
      /**
       *  repo scope description.
       *
       * 	Grants full access to private and public repositories.
       * 	That includes read/write access to code, commit statuses,
       * 	repository and organization projects, invitations, collaborators,
       * 	adding team memberships, deployment statuses, and repository webhooks
       * 	for public and private repositories and organizations.
       * 	Also grants ability to manage user projects.
       *
       */
      if(scopesList.contains("repo")) {
        return true;
        /**
         *  public_repo scope description.
         *
         *  Limits access to public repositories.
         *  That includes read/write access to code, commit statuses,
         *  repository projects, collaborators, and deployment statuses
         *  for public repositories and organizations.
         *  Also required for starring public repositories.
         */
      } else if(!repo.isPrivate() && scopesList.contains("public_repo")) {
        return true;
      } else {
        return false;
      }
    }
  }

  private RepositoryService getRepositoryService(GitHubClient client) {
    return new RepositoryService(client);
  }

  private OAuthService getOAuthService(GitHubClient client) {
    return new OAuthService(client);
  }
  
  private User getLoginUser(GitHubClient client) throws IOException {
    UserService userService = new UserService(client);
    return userService.getUser();
  }
  
  private List<RepositoryBranch> getBranches(Repository repository, RepositoryService repositoryService)
      throws IOException {
    return repositoryService.getBranches(repository);
  }
  
  private Repository getRepository(String repository, RepositoryService repositoryService)
      throws IOException {
    return repositoryService.getRepository(getOrganizationName(repository), getRepositoryName(repository));
  }

  private Repository getRepositoryForOauthUser(String organization, String repository,
                                               RepositoryService repositoryService)
    throws ServiceException, IOException {
    List<Repository> repos = repositoryService.getRepositories();
    for (Repository repo : repos) {
      String currentRepoOrganization = getOrganizationName(repo.getCloneUrl());
      if (organization.equals(currentRepoOrganization) && repository.equals(repo.getName())) {
        return repo;
      }
    }
    throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.SEVERE,
      "Could not find remote repository " + repository + " for organization " + organization +
        " on GitHub when listing repositories for the supplied API key");
  }
  
  private String getRepositoryName(String remoteURI) {
    Matcher matcher = REPO_ATTRS.matcher(remoteURI);
    if (matcher.matches()) {
      return matcher.group("repository");
    }
    throw new IllegalArgumentException("Could not parse remote URI: " + remoteURI);
  }

  private String getOrganizationName(String remoteURI) {
    Matcher matcher = REPO_ATTRS.matcher(remoteURI);
    if (matcher.matches()) {
      return matcher.group("organization");
    }
    throw new IllegalArgumentException("Could not parse remote URI: " + remoteURI);
  }
  
  private String getHost(String remoteURI) {
    Matcher matcher = REPO_ATTRS.matcher(remoteURI);
    if (matcher.matches()) {
      String hostName = matcher.group("type");
      if ("github.com".equals(hostName)) {
        hostName = "api." + hostName;
      }
      return hostName;
    }
    throw new IllegalArgumentException("Could not parse remote URI: " + remoteURI);
  }
}
