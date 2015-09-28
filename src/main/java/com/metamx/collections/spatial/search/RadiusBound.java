/*
 * Copyright 2011 - 2015 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metamx.collections.spatial.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.metamx.collections.spatial.ImmutablePoint;

/**
 */
public class RadiusBound extends RectangularBound
{
	private static float[] getMinCoords(float[] coords, float radius)
	{
		float[] retVal = new float[coords.length];
		for (int i = 0; i < coords.length; i++) {
			retVal[i] = coords[i] - radius;
		}
		return retVal;
	}

	private static float[] getMaxCoords(float[] coords, float radius)
	{
		float[] retVal = new float[coords.length];
		for (int i = 0; i < coords.length; i++) {
			retVal[i] = coords[i] + radius;
		}
		return retVal;
	}

	private final float[]	coords;
	private final float		radius;

	@JsonCreator
	public RadiusBound(
		@JsonProperty("coords") float[] coords,
		@JsonProperty("radius") float radius,
		@JsonProperty("limit") int limit)
	{
		super(getMinCoords(coords, radius), getMaxCoords(coords, radius), limit);

		this.coords = coords;
		this.radius = radius;
	}

	public RadiusBound(
		float[] coords,
		float radius)
	{
		this(coords, radius, 0);
	}

	@JsonProperty
	public float[] getCoords()
	{
		return coords;
	}

	@JsonProperty
	public float getRadius()
	{
		return radius;
	}

	@Override
	public boolean contains(float[] otherCoords)
	{
		//    if (super.contains(otherCoords)) {
		if (coords.length == 2) {
			double toRadian = Math.PI / 180;
			// lat=[0] lon=[1]
			double x = (otherCoords[1] - coords[1]) * toRadian * Math.cos((coords[0] + otherCoords[0]) * toRadian / 2);
			double y = (otherCoords[0] - coords[0]) * toRadian;
			double d = Math.sqrt(x * x + y * y) * 6371000.0f;
			return (d <= radius);
		} else {
			double total = 0.0;
			for (int i = 0; i < coords.length; i++) {
				total += Math.pow(otherCoords[i] - coords[i], 2);
			}
			return (total <= Math.pow(radius, 2));
		}
		//	}
		//	return false;
	}

	@Override
	public Iterable<ImmutablePoint> filter(Iterable<ImmutablePoint> points)
	{
		return Iterables.filter(
			points,
			new Predicate<ImmutablePoint>()
			{
				@Override
				public boolean apply(ImmutablePoint point)
				{
					return contains(point.getCoords());
				}
			}
			);
	}
}
