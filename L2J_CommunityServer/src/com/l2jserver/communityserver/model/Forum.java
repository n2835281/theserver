/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.communityserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.communityserver.L2DatabaseFactory;
import com.l2jserver.communityserver.model.Topic.ConstructorType;

public class Forum
{
	// SQL
	private static final String INSERT_FORUM = "INSERT INTO forums (serverId,forum_id,forum_name,forum_type,forum_owner_id) values (?,?,?,?,?)";
	private static final String GET_TOPICS = "SELECT * FROM topics WHERE serverId=? AND topic_forum_id=? ORDER BY topic_id DESC";
	private static final String GET_FORUMS = "SELECT * FROM forums WHERE serverId=? AND forum_id=?";
	
	// type
	public static final int ROOT = 0;
	public static final int NORMAL = 1;
	public static final int CLAN = 2;
	public static final int PLAYER = 3;
	
	private static Logger _log = Logger.getLogger(Forum.class.getName());
	private final Map<Integer, Topic> _topic;
	private final int _forumId;
	private final int _sqlDPId;
	private String _forumName;
	private int _forumType;
	private int _ownerID;
	private boolean _loaded = false;
	
	/**
	 * @param sqlDPId
	 * @param Forumid
	 */
	public Forum(final int sqlDPId, int Forumid)
	{
		_sqlDPId = sqlDPId;
		_forumId = Forumid;
		_topic = new FastMap<>();
	}
	
	/**
	 * @param sqlDPId
	 * @param Forumid
	 * @param name
	 * @param type
	 * @param OwnerID
	 */
	public Forum(final int sqlDPId, int Forumid, String name, int type, int OwnerID)
	{
		_sqlDPId = sqlDPId;
		_forumName = name;
		_forumId = Forumid;
		_forumType = type;
		_ownerID = OwnerID;
		_topic = new FastMap<>();
		_loaded = true;
		insertindb();
		if (type == Forum.PLAYER)
		{
			_topic.put(Topic.INBOX, new Topic(ConstructorType.CREATE, _sqlDPId, Topic.INBOX, Forumid, name, OwnerID, 0));
			_topic.put(Topic.OUTBOX, new Topic(ConstructorType.CREATE, _sqlDPId, Topic.OUTBOX, Forumid, name, OwnerID, 0));
			_topic.put(Topic.ARCHIVE, new Topic(ConstructorType.CREATE, _sqlDPId, Topic.ARCHIVE, Forumid, name, OwnerID, 0));
			_topic.put(Topic.TEMP_ARCHIVE, new Topic(ConstructorType.CREATE, _sqlDPId, Topic.TEMP_ARCHIVE, Forumid, name, OwnerID, 0));
			_topic.put(Topic.MEMO, new Topic(ConstructorType.CREATE, _sqlDPId, Topic.MEMO, Forumid, name, OwnerID, 0));
		}
		else if (type == Forum.CLAN)
		{
			_topic.put(Topic.ANNOUNCE, new Topic(ConstructorType.CREATE, _sqlDPId, Topic.ANNOUNCE, Forumid, name, OwnerID, 0));
			_topic.put(Topic.BULLETIN, new Topic(ConstructorType.CREATE, _sqlDPId, Topic.BULLETIN, Forumid, name, OwnerID, 0));
		}
	}
	
	private void load()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(GET_FORUMS))
		{
			statement.setInt(1, _sqlDPId);
			statement.setInt(2, _forumId);
			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					_forumName = result.getString("forum_name");
					_forumType = Integer.parseInt(result.getString("forum_type"));
					_ownerID = Integer.parseInt(result.getString("forum_owner_id"));
				}
			}
		}
		catch (Exception e)
		{
			_log.warning("data error on Forum " + _forumId + " : " + e);
			e.printStackTrace();
		}
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(GET_TOPICS))
		{
			statement.setInt(1, _sqlDPId);
			statement.setInt(2, _forumId);
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					Topic t = new Topic(Topic.ConstructorType.RESTORE, _sqlDPId, Integer.parseInt(result.getString("topic_id")), Integer.parseInt(result.getString("topic_forum_id")), result.getString("topic_name"), Integer.parseInt(result.getString("topic_ownerid")), Integer.parseInt(result.getString("topic_permissions")));
					_topic.put(t.getID(), t);
				}
			}
		}
		catch (Exception e)
		{
			_log.warning("data error on Forum " + _forumId + " : " + e);
			e.printStackTrace();
		}
	}
	
	public int getTopicSize()
	{
		if (!_loaded)
		{
			load();
			_loaded = true;
		}
		return _topic.size();
	}
	
	public Topic gettopic(int j)
	{
		if (!_loaded)
		{
			load();
			_loaded = true;
		}
		return _topic.get(j);
	}
	
	public void addtopic(Topic t)
	{
		if (!_loaded)
		{
			load();
			_loaded = true;
		}
		_topic.put(t.getID(), t);
	}
	
	/**
	 * @return
	 */
	public int getID()
	{
		return _forumId;
	}
	
	public int getOwner()
	{
		return _ownerID;
	}
	
	public String getName()
	{
		if (!_loaded)
		{
			load();
			_loaded = true;
		}
		return _forumName;
	}
	
	public int getType()
	{
		if (!_loaded)
		{
			load();
			_loaded = true;
		}
		return _forumType;
	}
	
	/**
	 * @param id
	 */
	public void rmTopicByID(int id)
	{
		_topic.remove(id);
		
	}
	
	public final int getSqlDPId()
	{
		return _sqlDPId;
	}
	
	public void insertindb()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_FORUM))
		{
			// TODO: needs to be changed
			statement.setInt(1, _sqlDPId);
			statement.setInt(2, _forumId);
			statement.setString(3, _forumName);
			statement.setInt(4, _forumType);
			statement.setInt(5, _ownerID);
			statement.execute();
		}
		catch (Exception e)
		{
			_log.warning("error while saving new Forum to db " + e);
		}
	}
	
	public void vload()
	{
		if (!_loaded)
		{
			load();
			_loaded = true;
		}
	}
}
