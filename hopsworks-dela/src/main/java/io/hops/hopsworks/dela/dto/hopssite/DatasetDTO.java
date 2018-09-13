/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.dela.dto.hopssite;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

public class DatasetDTO implements Serializable {

  @XmlRootElement
  public static class Proto {
    private String name;
    private String description;
    private Collection<String> categories;
    private long size;
    private String userEmail;

    public Proto() {
    }

    public Proto(String name, String description, Collection<String> categories, long size, 
      String userEmail) {
      this.name = name;
      this.description = description;
      this.categories = categories;
      this.userEmail = userEmail;
      this.size = size;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Collection<String> getCategories() {
      return categories;
    }

    public void setCategories(Collection<String> categories) {
      this.categories = categories;
    }

    public long getSize() {
      return size;
    }

    public void setSize(long size) {
      this.size = size;
    }

    public String getUserEmail() {
      return userEmail;
    }

    public void setUserEmail(String userEmail) {
      this.userEmail = userEmail;
    }
  }

  @XmlRootElement
  public static class Search implements Serializable {

    private String name;
    private int version;
    private String description;

    public Search() {
    }

    public Search(String name, int version, String description) {
      this.name = name;
      this.version = version;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getVersion() {
      return version;
    }

    public void setVersion(int version) {
      this.version = version;
    }
    
    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  @XmlRootElement
  public static class Details implements Serializable {

    private Owner owner;
    private Collection<String> categories;
    private Date publishedOn;
    private long size;
    private Health datasetHealth;

    public Details() {
    }

    public Details(Owner owner, Collection<String> categories, Date publishedOn, long size, Health datasetHealth) {
      this.owner = owner;
      this.categories = categories;
      this.publishedOn = publishedOn;
      this.size = size;
      this.datasetHealth = datasetHealth;
    }

    public Owner getOwner() {
      return owner;
    }

    public void setOwner(Owner owner) {
      this.owner = owner;
    }

    public Collection<String> getCategories() {
      return categories;
    }

    public void setCategories(Collection<String> categories) {
      this.categories = categories;
    }

    public Date getPublishedOn() {
      return publishedOn;
    }

    public void setPublishedOn(Date publishedOn) {
      this.publishedOn = publishedOn;
    }

    public long getSize() {
      return size;
    }

    public void setSize(long size) {
      this.size = size;
    }

    public Health getDatasetHealth() {
      return datasetHealth;
    }

    public void setDatasetHealth(Health datasetHealth) {
      this.datasetHealth = datasetHealth;
    }
  }

  @XmlRootElement
  public static class Health implements Serializable {

    private int seeders;
    private int leechers;

    public Health() {
    }

    public Health(int seeders, int leechers) {
      this.seeders = seeders;
      this.leechers = leechers;
    }

    public int getSeeders() {
      return seeders;
    }

    public void setSeeders(int seeders) {
      this.seeders = seeders;
    }

    public int getLeechers() {
      return leechers;
    }

    public void setLeechers(int leechers) {
      this.leechers = leechers;
    }
  }

  @XmlRootElement
  public static class Complete implements Serializable {

    private Owner owner;
    private String name;
    private String description;
    private Collection<String> categories;
    private Date publishedOn;
    private int rating;
    private long size;

    public Complete() {
    }

    public Complete(Owner owner, String name, String description, Collection<String> categories, Date publishedOn,
      int rating, long size) {
      this.owner = owner;
      this.name = name;
      this.description = description;
      this.categories = categories;
      this.publishedOn = publishedOn;
      this.rating = rating;
      this.size = size;
    }

    public Owner getOwner() {
      return owner;
    }

    public void setOwner(Owner owner) {
      this.owner = owner;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Collection<String> getCategories() {
      return categories;
    }

    public void setCategories(Collection<String> categories) {
      this.categories = categories;
    }

    public Date getPublishedOn() {
      return publishedOn;
    }

    public void setPublishedOn(Date publishedOn) {
      this.publishedOn = publishedOn;
    }

    public int getRating() {
      return rating;
    }

    public void setRating(int rating) {
      this.rating = rating;
    }

    public long getSize() {
      return size;
    }

    public void setSize(long size) {
      this.size = size;
    }
  }

  @XmlRootElement
  public static class Owner implements Serializable {

    private String clusterDescription;
    private String userDescription;

    public Owner() {
    }

    public Owner(String clusterDescription, String userDescription) {
      this.clusterDescription = clusterDescription;
      this.userDescription = userDescription;
    }

    public String getClusterDescription() {
      return clusterDescription;
    }

    public void setClusterDescription(String clusterDescription) {
      this.clusterDescription = clusterDescription;
    }

    public String getUserDescription() {
      return userDescription;
    }

    public void setUserDescription(String userDescription) {
      this.userDescription = userDescription;
    }
  }
}
