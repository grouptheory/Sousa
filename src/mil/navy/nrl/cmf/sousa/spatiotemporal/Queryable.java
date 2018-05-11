package mil.navy.nrl.cmf.sousa.spatiotemporal;

//import javax.vecmath.Vector3d;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

/**
   <CODE>Queryable</CODE> is an interface to a spatiotemporal search
   engine.  Searches return <CODE>Sets</CODE> of {@link
   QueryResultHandle}.  One <CODE>Set</CODE> contains things that are
   no longer within the spatiotemporal search region. The other
   <CODE>Set</CODE> returns things that are new to the spatiotemporal
   search region.

   <P>

   As a spatiotemporal region moves around in space-time, it loses and
   acquires members.
*/

public interface Queryable {

	/**
	   Answers a spatiotemporal query and determines what objects are
	   no longer in the spatiotemporal region (<CODE>removed</CODE>),
	   what objects are new members of the region
	   (<CODE>added</CODE>), and what objects are still members but
	   with different values (<CODE>changed</CODE>).

	   <P>
	   By convention, the components of each <CODE>Vector3d</CODE>
	   encode the physical location in the following way:

	   <UL>
	   <LI><CODE>x</CODE> is degrees latitude.
	   <LI><CODE>y</CODE> is degrees longitude.
	   <LI><CODE>z</CODE> is elevation in meters relative to sealevel.
	   (For example, 0 is sealevel, -1 is 1 meter below sealevel, 1 is 1
	   meter above sealevel.)
	   <UL>
 
	   @param lowerLeftCorner spatial lower left corner of the search
	   region
	   @param width spatial width of the search region
	   @param timeLowerBound lower bound on time of the search region
	   @param timeUpperBound upper bound on time of the search region
	   @param previous the <CODE>Set</CODE> returned by the previous
	   call to <CODE>query</CODE>
	   @param fieldNames the keys of each <CODE>QueryResultHandle</CODE>
	   @param added the <CODE>QueryResultHandles</CODE> of things that
	   are new to the search region.
	   @param removed the <CODE>QueryResultHandles</CODE> of things that
	   are no longer in the search region.
	   @param changed the <CODE>QueryResultHandles</CODE> of things that
	   are still in the search region but with different values.
	   @param context a <CODE>Map</CODE> in which the implementation
	   keeps per-client state.  The caller must create the
	   <CODE>Map</CODE> and must not change it.
	   @return a <CODE>Set</CODE> of {@link QueryResultHandle}
	*/
    public Set query(Vector3d lowerLeftCorner, Vector3d width,
					 Calendar timeLowerBound, Calendar timeUpperBound,
					 Set previous, Set fieldNames,
					 Set added, Set removed, Set changed, 
					 Map context);
}
