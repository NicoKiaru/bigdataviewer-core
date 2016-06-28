/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.viewer.state;

import static bdv.viewer.DisplayMode.FUSED;
import static bdv.viewer.DisplayMode.SINGLE;
import static bdv.viewer.Interpolation.NEARESTNEIGHBOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import bdv.util.MipmapTransforms;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Description of everything required to render the current image, such as the
 * current timepoint, the visible and current sources and groups respectively,
 * the viewer transformation, etc.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class ViewerState
{
	private final ArrayList< SourceAndConverter< ? > > sources;

	private final ArrayList< SourceState > sourceStates;

	/**
	 * read-only view of {@link #sources}.
	 */
	private final List< SourceAndConverter< ? > > unmodifiableSources;

	/**
	 * read-only view of {@link #sourceStates}.
	 */
	private final List< SourceState > unmodifiableSourceStates;

	private final ArrayList< SourceGroup > groups;

	/**
	 * read-only view of {@link #groups}.
	 */
	private final List< SourceGroup > unmodifiableGroups;

	/**
	 * number of available timepoints.
	 */
	private int numTimepoints;

	/**
	 * Transformation set by the interactive viewer. Transforms from global
	 * coordinate system to viewer coordinate system.
	 */
	private final AffineTransform3D viewerTransform;

	/**
	 * Which interpolation method is currently used to render the display.
	 */
	private Interpolation interpolation;

	/**
	 * Is the display mode <em>single-source</em>? In <em>single-source</em>
	 * mode, only the current source (SPIM angle). Otherwise, in <em>fused</em>
	 * mode, all active sources are blended.
	 */
//	protected boolean singleSourceMode;
	/**
	 * TODO
	 */
	private DisplayMode displayMode;

	/**
	 * The index of the current source.
	 * (In single-source mode only the current source is shown.)
	 */
	private int currentSource;

	/**
	 * The index of the current group.
	 * (In single-group mode only the sources in the current group are shown.)
	 */
	private int currentGroup;

	/**
	 * which timepoint is currently shown.
	 */
	private int currentTimepoint;

	/**
	 *
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display.
	 * @param numTimePoints
	 *            number of available timepoints.
	 */
	public ViewerState( final List< SourceAndConverter< ? > > sources, final List< SourceGroup > sourceGroups, final int numTimePoints )
	{
		this.sources = new ArrayList<>( sources );
		unmodifiableSources = Collections.unmodifiableList( this.sources );

		sourceStates = new ArrayList<>( sources.size() );
		for ( int i = 0; i < sources.size(); ++i )
			sourceStates.add( new SourceState() );
		unmodifiableSourceStates = Collections.unmodifiableList( this.sourceStates );

		groups = new ArrayList< SourceGroup >( sourceGroups.size() );
		for ( final SourceGroup g : sourceGroups )
			groups.add( g.copy( this ) );
		unmodifiableGroups = Collections.unmodifiableList( this.groups );

		this.numTimepoints = numTimePoints;
		viewerTransform = new AffineTransform3D();
		interpolation = NEARESTNEIGHBOR;
		displayMode = SINGLE;
		currentSource = sources.isEmpty() ? -1 : 0;
		currentGroup = 0;
		currentTimepoint = 0;
	}

	/**
	 * copy constructor
	 * @param s
	 */
	protected ViewerState( final ViewerState s )
	{
		sources = new ArrayList<>( s.sources );
		unmodifiableSources = Collections.unmodifiableList( sources );

		sourceStates = new ArrayList<>( sources.size() );
		for ( final SourceState state : s.sourceStates )
			sourceStates.add( state.copy() );
		unmodifiableSourceStates = Collections.unmodifiableList( this.sourceStates );

		groups = new ArrayList< SourceGroup >( s.groups.size() );
		for ( final SourceGroup group : s.groups )
			this.groups.add( group.copy( this ) );
		unmodifiableGroups = Collections.unmodifiableList( groups );

		numTimepoints = s.numTimepoints;
		viewerTransform = s.viewerTransform.copy();
		interpolation = s.interpolation;
		displayMode = s.displayMode;
		currentSource = s.currentSource;
		currentGroup = s.currentGroup;
		currentTimepoint = s.currentTimepoint;
	}

	public ViewerState copy()
	{
		return new ViewerState( this );
	}


	/*
	 * Renderer state.
	 * (which sources to show, which interpolation method to use, etc.)
	 */

	/**
	 * Get the viewer transform.
	 *
	 * @param t is set to the viewer transform.
	 */
	public synchronized void getViewerTransform( final AffineTransform3D t )
	{
		t.set( viewerTransform );
	}

	/**
	 * Set the viewer transform.
	 *
	 * @param t transform parameters.
	 */
	public synchronized void setViewerTransform( final AffineTransform3D t )
	{
		viewerTransform.set( t );
	}

	/**
	 * Get the index of the current source.
	 */
	public synchronized int getCurrentSource()
	{
		return currentSource;
	}

	/**
	 * Make the source with the given index current.
	 */
	public synchronized void setCurrentSource( final int index )
	{
		final int minIndex = sources.isEmpty() ? -1 : 0;
		if ( index >= minIndex && index < sources.size() )
			currentSource = index;
	}

	/**
	 * Make the given source current.
	 */
	public synchronized void setCurrentSource( final Source< ? > source )
	{
		final int i = getSourceIndex( source );
		if ( i >= 0 )
			setCurrentSource( i );
	}

	/**
	 * Check whether the source with the given index is active (visible in fused
	 * mode).
	 *
	 * @return {@code true} if the source with the given index is active.
	 */
	public synchronized boolean isSourceActive( final int index )
	{
		if ( index < 0 || index >= sources.size() )
			return false;

		return sourceStates.get( index ).isActive();
	}

	/**
	 * Make the source with the given index active (visible in fused mode) or
	 * inactive.
	 */
	public synchronized void setSourceActive( final int index, final boolean isActive )
	{
		if ( index < 0 || index >= sources.size() )
			return;

		sourceStates.get( index ).setActive( isActive );

	}

	/**
	 * Make the given source active (visible in fused mode) or inactive.
	 */
	public synchronized void setSourceActive( final Source< ? > source, final boolean isActive )
	{
		final int i = getSourceIndex( source );
		if ( i >= 0 )
			setSourceActive( i, isActive );
	}


	/**
	 * Get the index of the current group.
	 */
	public synchronized int getCurrentGroup()
	{
		return currentGroup;
	}

	/**
	 * Make the group with the given index current.
	 */
	public synchronized void setCurrentGroup( final int index )
	{
		if ( index < 0 || index >= groups.size() )
			return;

		currentGroup = index;
	}

	/**
	 * Check whether the group with the given index is active (visible in fused
	 * mode).
	 *
	 * @return {@code true} if the group with the given index is active.
	 */
	public synchronized boolean isGroupActive( final int index )
	{
		if ( index < 0 || index >= groups.size() )
			return false;

		return groups.get( index ).isActive();
	}

	/**
	 * Make the group with the given index active (visible in fused mode) or
	 * inactive.
	 */
	public synchronized void setGroupActive( final int index, final boolean isActive )
	{
		if ( index < 0 || index >= groups.size() )
			return;

		groups.get( index ).setActive( isActive );

	}

	/**
	 * Get the interpolation method.
	 *
	 * @return interpolation method.
	 */
	public synchronized Interpolation getInterpolation()
	{
		return interpolation;
	}

	/**
	 * Set the interpolation method.
	 *
	 * @param method interpolation method.
	 */
	public synchronized void setInterpolation( final Interpolation method )
	{
		interpolation = method;
	}

	/**
	 * DEPRECATED. Replace by {@link #getDisplayMode()}.
	 */
	@Deprecated
	public synchronized boolean isSingleSourceMode()
	{
		return displayMode == SINGLE;
	}

	/**
	 * DEPRECATED. Replace by {@link #setDisplayMode(DisplayMode)}.
	 */
	@Deprecated
	public synchronized void setSingleSourceMode( final boolean singleSourceMode )
	{
		if ( singleSourceMode )
			setDisplayMode( SINGLE );
		else
			setDisplayMode( FUSED );
	}

	/**
	 * Set the display mode. In <em>single-source</em> mode, only the current
	 * source (SPIM angle) is shown. In <em>fused</em> mode, all active sources
	 * are blended. In <em>single-group mode</em>, all sources in the current
	 * group are blended. In <em>fused group mode</em> all sources in all active
	 * groups are blended.
	 */
	public synchronized void setDisplayMode( final DisplayMode mode )
	{
		displayMode = mode;
	}

	/**
	 * Get the display mode. In <em>single-source</em> mode, only the current
	 * source (SPIM angle) is shown. In <em>fused</em> mode, all active sources
	 * are blended. In <em>single-group mode</em>, all sources in the current
	 * group are blended. In <em>fused group mode</em> all sources in all active
	 * groups are blended.
	 *
	 * @return the current display mode.
	 */
	public synchronized DisplayMode getDisplayMode()
	{
		return displayMode;
	}

	/**
	 * Get the timepoint index that is currently displayed.
	 *
	 * @return current timepoint index
	 */
	public synchronized int getCurrentTimepoint()
	{
		return currentTimepoint;
	}

	/**
	 * Set the current timepoint index.
	 *
	 * @param timepoint
	 *            timepoint index.
	 */
	public synchronized void setCurrentTimepoint( final int timepoint )
	{
		currentTimepoint = timepoint;
	}

	/**
	 * Returns a list of all sources.
	 *
	 * @return list of all sources.
	 */
	public List< ? extends SourceAndConverter< ? > > getSources()
	{
		return unmodifiableSources;
	}

	/**
	 * Returns a list of all {@link SourceState}s.
	 * These store whether sources are active.
	 *
	 * @return list of all {@link SourceState}s.
	 */
	public List< SourceState > getSourceStates()
	{
		return unmodifiableSourceStates;
	}

	/**
	 * Returns the number of sources.
	 *
	 * @return number of sources.
	 */
	public int numSources()
	{
		return sources.size();
	}

	/**
	 * Returns a list of all source groups.
	 *
	 * @return list of all source groups.
	 */
	public List< SourceGroup > getSourceGroups()
	{
		return unmodifiableGroups;
	}

	/**
	 * Returns the number of source groups.
	 *
	 * @return number of source groups.
	 */
	public int numSourceGroups()
	{
		return groups.size();
	}

	public synchronized void addSource( final SourceAndConverter< ? > source )
	{
		sources.add( source );
		sourceStates.add( new SourceState() );
		if ( currentSource < 0 )
			currentSource = 0;
	}

	public synchronized void removeSource( final Source< ? > source )
	{
		final int i = getSourceIndex( source );
		if ( i >= 0 )
			removeSource( i );
	}

	protected void removeSource( final int index )
	{
		sources.remove( index );
		sourceStates.remove( index );
		if ( sources.isEmpty() )
			currentSource = -1;
		else if ( currentSource == index )
			currentSource = 0;
		else if ( currentSource > index )
			--currentSource;
		for( final SourceGroup group : groups )
		{
			final SortedSet< Integer > ids = group.getSourceIds();
			final ArrayList< Integer > oldids = new ArrayList< Integer >( ids );
			ids.clear();
			for ( final int id : oldids )
			{
				if ( id < index )
					ids.add( id );
				else if ( id > index )
					ids.add( id - 1 );
			}
		}
	}

	public synchronized boolean isSourceVisible( final int index )
	{
		switch ( displayMode )
		{
		case SINGLE:
			return ( index == currentSource ) && isPresent( index );
		case GROUP:
			return groups.get( currentGroup ).getSourceIds().contains( index ) && isPresent( index );
		case FUSED:
			return sourceStates.get( index ).isActive() && isPresent( index );
		case FUSEDGROUP:
		default:
			for ( final SourceGroup group : groups )
				if ( group.isActive() && group.getSourceIds().contains( index ) && isPresent( index ) )
					return true;
			return false;
		}
	}

	private boolean isPresent( final int sourceId )
	{
		return sources.get( sourceId ).getSpimSource().isPresent( currentTimepoint );
	}

	/**
	 * Returns a list of the indices of all currently visible sources.
	 *
	 * @return indices of all currently visible sources.
	 */
	public synchronized List< Integer > getVisibleSourceIndices()
	{
		final ArrayList< Integer > visible = new ArrayList< Integer >();
		switch ( displayMode )
		{
		case SINGLE:
			if ( currentSource >= 0 && isPresent( currentSource ) )
				visible.add( currentSource );
			break;
		case GROUP:
			for ( final int sourceId : groups.get( currentGroup ).getSourceIds() )
				if ( isPresent( sourceId ) )
					visible.add( sourceId );
			break;
		case FUSED:
			for ( int i = 0; i < sources.size(); ++i )
				if ( sourceStates.get( i ).isActive() && isPresent( i ) )
					visible.add( i );
			break;
		case FUSEDGROUP:
			final TreeSet< Integer > gactive = new TreeSet< Integer >();
			for ( final SourceGroup group : groups )
				if ( group.isActive() )
					gactive.addAll( group.getSourceIds() );
			for ( final int sourceId : new ArrayList< Integer >( gactive ) )
				if ( isPresent( sourceId ) )
					visible.add( sourceId );
			break;
		}
		return visible;
	}

	/*
	 * Utility methods.
	 */

	/**
	 * Get the mipmap level that best matches the given screen scale for the given source.
	 *
	 * @param screenScaleTransform
	 *            screen scale, transforms screen coordinates to viewer coordinates.
	 * @return mipmap level
	 */
	public synchronized int getBestMipMapLevel( final AffineTransform3D screenScaleTransform, final int sourceIndex )
	{
		final AffineTransform3D screenTransform = new AffineTransform3D();
		getViewerTransform( screenTransform );
		screenTransform.preConcatenate( screenScaleTransform );

		final Source< ? > source = sources.get( sourceIndex ).getSpimSource();

		return MipmapTransforms.getBestMipMapLevel( screenTransform, source, currentTimepoint );
	}

	/**
	 * Get the number of timepoints.
	 *
	 * @return the number of timepoints.
	 */
	public synchronized int getNumTimepoints()
	{
		return numTimepoints;
	}

	/**
	 * Set the number of timepoints.
	 *
	 * @param numTimepoints
	 *            the number of timepoints.
	 */
	public synchronized void setNumTimepoints( final int numTimepoints )
	{
		this.numTimepoints = numTimepoints;
	}

	/**
	 * DON'T USE THIS.
	 * <p>
	 * This is a work around for JDK bug
	 * https://bugs.openjdk.java.net/browse/JDK-8029147 which leads to
	 * ViewerPanel not being garbage-collected when ViewerFrame is closed. So
	 * instead we need to manually let go of resources...
	 */
	public void kill()
	{
		sources.clear();
		groups.clear();
	}

	/**
	 * Get index of (first) {@link SourceState} that matches the given
	 * {@link Source}.
	 */
	private int getSourceIndex( final Source< ? > source )
	{
		for ( int i = 0; i < sources.size(); ++i )
		{
			final SourceAndConverter< ? > s = sources.get( i );
			if ( s.getSpimSource() == source )
				return i;
		}
		return -1;
	}
}
