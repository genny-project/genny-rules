package life.genny.utils;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import life.genny.models.GennyToken;
import life.genny.qwanda.Link;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.utils.TableUtils;
import life.genny.utils.VertxUtils;
import life.genny.utils.BaseEntityUtils;

public class DropdownUtils implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SearchEntity searchEntity;
	private GennyToken serviceToken;

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public DropdownUtils(GennyToken serviceToken) {
		this.serviceToken = serviceToken;
	}

	public SearchEntity setNewSearch(String code, String name) {

		searchEntity = new SearchEntity(code, name);
		return this.searchEntity;
	}

	public void setSearch(SearchEntity sbe) {

		this.searchEntity = sbe;
	}

	public QDataBaseEntityMessage sendSearchResultsUsingAltSearch(String parentCode, String linkCode, String linkValue,
			GennyToken userToken, Boolean sortByWeight, Boolean cache) throws IOException {

		return this.sendSearchResults(parentCode, linkCode, linkValue, true, false, userToken, sortByWeight, cache, true);
	}

	public QDataBaseEntityMessage sendSearchResults(String parentCode, String linkCode, String linkValue,
			GennyToken userToken) throws IOException {

		return this.sendSearchResults(parentCode, linkCode, linkValue, userToken, false);
	}

	public QDataBaseEntityMessage sendSearchResults(String parentCode, String linkCode, String linkValue,
			GennyToken userToken, Boolean sortByWeight) throws IOException {

		return this.sendSearchResults(parentCode, linkCode, linkValue, true, false, userToken, sortByWeight, false, false);
	}

	public QDataBaseEntityMessage sendSearchResults(String parentCode, String linkCode, String linkValue,
			GennyToken userToken, Boolean sortByWeight, Boolean cache) throws IOException {

		return this.sendSearchResults(parentCode, linkCode, linkValue, true, false, userToken, sortByWeight, cache, false);
	}

	public QDataBaseEntityMessage sendSearchResults(String parentCode, String linkCode, String linkValue, Boolean replace,
			Object shouldDeleteLinkedBaseEntities, GennyToken userToken, Boolean sortByWeight) throws IOException {
		return sendSearchResults(parentCode, linkCode, linkValue, replace, shouldDeleteLinkedBaseEntities, userToken,
				sortByWeight, false, false);
	}

	public QDataBaseEntityMessage sendSearchResults(String parentCode, String linkCode, String linkValue, Boolean replace,
			Object shouldDeleteLinkedBaseEntities, GennyToken userToken, Boolean sortByWeight, Boolean cache, Boolean useSearchAlt)
			throws IOException {
		QDataBaseEntityMessage beMessage = getSearchResults(this.searchEntity, parentCode, linkCode, linkValue, replace,
				shouldDeleteLinkedBaseEntities, userToken, this.serviceToken, sortByWeight, useSearchAlt);

		if (beMessage != null) {

			/* Writing to Vert.x EventBus */
			if (cache) {

			} else {
				beMessage.setToken(userToken.getToken());
				writeToVertx("webcmds", beMessage);
			}
		} else {
			log.error("Could not retrieve Search Results for parentCode " + parentCode + " and linkCode " + linkCode
					+ " and linkValue " + linkValue);
		}
		return beMessage;
	}

	/*
	 * Get Search results. Returns QDataBaseEntityMessage
	 */
	private QDataBaseEntityMessage getSearchResults(SearchEntity searchBE, String parentCode, String linkCode,
			String linkValue, Boolean replace, Object shouldDeleteLinkedBaseEntities, GennyToken userToken,
			GennyToken serviceToken, Boolean sortByWeight, Boolean useSearchAlt) throws IOException {

		String token = userToken.getToken();
		
		BaseEntityUtils beUtils = new BaseEntityUtils(serviceToken, userToken);
		TableUtils tableUtils = new TableUtils(beUtils);
		SearchUtils searchUtils = new SearchUtils(beUtils);
		
		List<EntityAttribute> filters = tableUtils.getUserFilters(searchBE);

		if (!filters.isEmpty()) {
			log.info("User Filters are NOT empty");
			log.info("Adding User Filters to searchBe  ::  " + searchBE.getCode());
			for (EntityAttribute filter : filters) {
				searchBE.getBaseEntityAttributes().add(filter);
			}
		}

		// Check if present in cache
		// TODO THESE CACHES NEED TO BE CLEARED UPON ANY ADDITIONS
		QDataBaseEntityMessage DROPDOWN_MSG = VertxUtils.getObject(serviceToken.getRealm(), "", searchBE.getCode() + "_MSG",
				QDataBaseEntityMessage.class, serviceToken.getToken());

		if (DROPDOWN_MSG == null || true) { // TODO, update search caches upon item add/delete

			QDataBaseEntityMessage msg = null;

			if (GennySettings.searchAlt && useSearchAlt) {
				System.out.println("Using alt search for dropdown");
				// msg = tableUtils.searchUsingHql(serviceToken, searchBE, msg);
				msg = searchUtils.searchUsingSearch25(serviceToken, searchBE);

			} else {
				System.out.println("Using standard search for dropdown");
				String jsonSearchBE = JsonUtils.toJson(searchBE);
				String resultJson = QwandaUtils.apiPostEntity(GennySettings.qwandaServiceUrl + "/qwanda/baseentitys/search",
						jsonSearchBE, serviceToken.getToken());

				msg = JsonUtils.fromJson(resultJson, QDataBaseEntityMessage.class);
			}

			if (msg != null) {
				msg.setParentCode(parentCode);
				msg.setToken(token);
				msg.setLinkCode(linkCode);
				msg.setLinkValue(linkValue);
				msg.setReplace(replace);
				msg.setShouldDeleteLinkedBaseEntities(shouldDeleteLinkedBaseEntities);

				/* Linking child baseEntity to the parent baseEntity */
				QDataBaseEntityMessage beMessage = setDynamicLinksToParentBe(msg, parentCode, linkCode, linkValue, userToken,
						sortByWeight);
				VertxUtils.putObject(serviceToken.getRealm(), "", searchBE.getCode() + "_MSG", beMessage,
						serviceToken.getToken());
				return beMessage;

			} else {

				return null;
			}
		} else {
			log.info("Dropdown Utils: used cached! " + searchBE.getCode() + "_MSG");
			return DROPDOWN_MSG;
		}

	}

	private double getWeight(BaseEntity be, String parentCode) {

		double weight = 1.0;

		try {
			Set<EntityEntity> entities = be.getLinks();

			for (EntityEntity entity : entities) {

				if (entity.getLink().getSourceCode().equals(parentCode)) {

					weight = entity.getLink().getWeight();
					break;
				}
			}
		} catch (Exception e) {

			// e.printStackTrace();
		}

		return weight;
	}

	private List<EntityEntity> sortChildLinksByWeight(BaseEntity parentBe) {
		Set<EntityEntity> childLinks = parentBe.getLinks();
		List<EntityEntity> sortedChildLinks = childLinks.stream().sorted(Comparator.comparing(EntityEntity::getWeight))
				.collect(Collectors.toList());
		return sortedChildLinks;
	}

	private BaseEntity[] sortBaseEntityByWeight(BaseEntity[] items, String parentCode,
			List<EntityEntity> sortedChildLinks) {
		// Set sorted links
		BaseEntity[] newItems = new BaseEntity[0];
		ArrayList<BaseEntity> newItemsList = new ArrayList<>();

		HashMap<String, BaseEntity> beMapping = new HashMap<>();
		for (BaseEntity be : items) {
			String beCode = be.getCode();
			beMapping.put(beCode, be);
		}

		int index = 0;
		for (EntityEntity ee : sortedChildLinks) {
			String targetCode = ee.getLink().getTargetCode();
			if (beMapping.containsKey(targetCode)) {
				beMapping.get(targetCode).setIndex(index);
				newItemsList.add(beMapping.get(targetCode));
//				newItems[index] = beMapping.get(targetCode);
				index++;
			} else {
				log.error(String.format("Parent Code %s doesn't have Link code %s", parentCode, targetCode));
			}
		}
		return newItemsList.toArray(newItems);
	}

	/*
	 * Setting dynamic links between parents and child. ie. linking DropDown items
	 * to the DropDown field.
	 */
	QDataBaseEntityMessage setDynamicLinksToParentBe(QDataBaseEntityMessage beMsg, String parentCode, String linkCode,
			String linkValue, GennyToken gennyToken, Boolean sortByWeight) {
		BaseEntity parentBe = new BaseEntityUtils(gennyToken).getBaseEntityByCode(parentCode);
		if (parentBe != null) {
			Set<EntityEntity> childLinks = new LinkedHashSet<>();
			double index = -1.0;
			/* creating a dumb attribute for linking the search results to the parent */
			Attribute attributeLink = new Attribute(linkCode, linkCode, new DataType(String.class));

			for (BaseEntity be : beMsg.getItems()) {
				// Sort items based on weight
				if (sortByWeight) {
					if (parentBe.getLinks().size() > 0) {
						List<EntityEntity> sortedChildLinks = sortChildLinksByWeight(parentBe);
						// update links
						parentBe.setLinks(new LinkedHashSet<>(sortedChildLinks));

						BaseEntity[] sortedItems = sortBaseEntityByWeight(beMsg.getItems(), parentBe.getCode(), sortedChildLinks);
						beMsg.setItems(sortedItems);
					}
					beMsg.add(parentBe);
					return beMsg;
				} else {
					index++;
					childLinks = new HashSet<>();
					parentBe.setLinks(childLinks);
					EntityEntity ee = new EntityEntity(parentBe, be, attributeLink, index);
					/* creating link for child */
					Link link = new Link(parentCode, be.getCode(), attributeLink.getCode(), linkValue, index);
					/* adding link */
					ee.setLink(link);
					/* adding child link to set of links */
					childLinks.add(ee);
				}
			}
			parentBe.setLinks(childLinks);
			beMsg.add(parentBe);
			return beMsg;
		} else {
			log.error("Unable to fetch Parent BaseEntity : parentCode");
			return null;
		}
	}

	private void writeToVertx(String channel, QDataBaseEntityMessage msg) {

		/* ATM sending form here might have to update later */
		VertxUtils.writeMsg(channel, JsonUtils.toJson(msg));
	}

}
