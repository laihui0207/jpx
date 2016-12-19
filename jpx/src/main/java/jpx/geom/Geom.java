/*
 * Java GPX Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */
package jpx.geom;

import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.atan;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;
import static java.util.Objects.requireNonNull;

import jpx.Length;
import jpx.Point;

/**
 * Implementation of <em>geodetic</em> functions.
 *
 * @see Ellipsoid
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @version !__version__!
 * @since !__version__!
 */
public final class Geom {

	public static final Geom WGSC_84 = of(Ellipsoid.WGSC_84);

	public static final Geom DEFAULT = WGSC_84;

	private final Ellipsoid _ellipsoid;

	// Major semi-axes of the ellipsoid.
	private final double A;
	private final double AA;

	// Minor semi-axes of the ellipsoid.
	private final double B;
	private final double BB;

	private final double AABBBB;

	// Flattening (A - B)/A
	private final double F;

	// The maximal iteration of the 'distance'
	private static final int DISTANCE_ITERATION_MAX = 20;

	// The epsilon of the result, when to stop iteration.
	private static final double DISTANCE_ITERATION_EPSILON = 1E-12;

	private Geom(final Ellipsoid ellipsoid) {
		_ellipsoid = requireNonNull(ellipsoid);

		A = ellipsoid.A();
		AA = A*A;
		B = ellipsoid.B();
		BB = B*B;
		AABBBB = (AA - BB)/BB;
		F = 1.0/ellipsoid.F();
	}

	public Ellipsoid getEllipsoid() {
		return _ellipsoid;
	}

	/**
	 * Calculate the distance between points on an ellipsoidal earth model.
	 *
	 * @see <a href="http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf">DIRECT AND
	 *               INVERSE SOLUTIONS OF GEODESICS 0 THE ELLIPSOID
	 *               WITH APPLICATION OF NESTED EQUATIONS</a>
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong-vincenty.html">
	 *     Vincenty solutions of geodesics on the ellipsoid</a>
	 *
	 * @param start the start point
	 * @param end the end point
	 * @return the distance between {@code start} and {@code end} in meters
	 * @throws NullPointerException if one of the points is {@code null}
	 */
	public Length distance(final Point start, final Point end) {
		final double lat1 = start.getLatitude().toRadians();
		final double lon1 = start.getLongitude().toRadians();
		final double lat2 = end.getLatitude().toRadians();
		final double lon2 = end.getLongitude().toRadians();

		final double omega = lon2 - lon1;

		final double tanphi1 = tan(lat1);
		final double tanU1 = (1.0 - F)*tanphi1;
		final double U1 = atan(tanU1);
		final double sinU1 = sin(U1);
		final double cosU1 = cos(U1);

		final double tanphi2 = tan(lat2);
		final double tanU2 = (1.0 - F)*tanphi2;
		final double U2 = atan(tanU2);
		final double sinU2 = sin(U2);
		final double cosU2 = cos(U2);

		final double sinU1sinU2 = sinU1*sinU2;
		final double cosU1sinU2 = cosU1*sinU2;
		final double sinU1cosU2 = sinU1*cosU2;
		final double cosU1cosU2 = cosU1*cosU2;

		// Eq. 13
		double lambda = omega;

		// Intermediates we'll need to compute distance 's'
		double a;
		double b;
		double sigma;
		double deltasigma;
		double lambda0;

		int iteration = 0;
		do {
			lambda0 = lambda;

			final double sinlambda = sin(lambda);
			final double coslambda = cos(lambda);

			// Eq. 14
			final double sin2sigma =
				(cosU2*sinlambda*cosU2*sinlambda) +
					(cosU1sinU2 - sinU1cosU2*coslambda)*
						(cosU1sinU2 - sinU1cosU2*coslambda);
			final double sinsigma = sqrt(sin2sigma);

			// Eq. 15
			final double cossigma = sinU1sinU2 + (cosU1cosU2*coslambda);

			// Eq. 16
			sigma = atan2(sinsigma, cossigma);

			// Eq. 17 Careful! sin2sigma might be almost 0!
			final double sinalpha = sin2sigma == 0.0
				? 0.0
				: cosU1cosU2*sinlambda/sinsigma;
			final double alpha = asin(sinalpha);
			final double cosalpha = cos(alpha);
			double cos2alpha = cosalpha*cosalpha;

			// Eq. 18 Careful! cos2alpha might be almost 0!
			final double cos2sigmam = cos2alpha == 0.0
				? 0.0
				: cossigma - 2*sinU1sinU2/cos2alpha;
			final double u2 = cos2alpha*AABBBB;

			final double cos2sigmam2 = cos2sigmam*cos2sigmam;

			// Eq. 3
			a = 1.0 + u2/16384*(4096 + u2*(-768 + u2*(320 - 175*u2)));

			// Eq. 4
			b = u2/1024*(256 + u2*(-128 + u2*(74 - 47*u2)));

			// Eq. 6
			deltasigma = b*sinsigma*(cos2sigmam +
				b/4*(cossigma*(-1 + 2 * cos2sigmam2) -
					b/6*cos2sigmam*(-3 + 4*sin2sigma)*(-3 + 4*cos2sigmam2)));

			// Eq. 10
			final double C = F/16*cos2alpha*(4 + F*(4 - 3*cos2alpha));

			// Eq. 11
			lambda = omega + (1 - C)*F*sinalpha*
				(sigma + C*sinsigma*(cos2sigmam +
					C*cossigma*(-1 + 2*cos2sigmam2)));

		} while (iteration++ < DISTANCE_ITERATION_MAX &&
			(abs((lambda - lambda0)/lambda) > DISTANCE_ITERATION_EPSILON));

		// Eq. 19
		final double s = B*a*(sigma - deltasigma);

		// The difference in elevation.
		final double e =
			start.getElevation().map(Length::toMeters).orElse(0.0) -
			end.getElevation().map(Length::toMeters).orElse(0.0);

		return Length.ofMeters(sqrt(s*s + e*e));
	}

	public static Geom of(final Ellipsoid ellipsoid) {
		return new Geom(ellipsoid);
	}

}