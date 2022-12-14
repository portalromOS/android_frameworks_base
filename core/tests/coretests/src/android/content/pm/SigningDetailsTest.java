/*
 * Copyright (C) 2020 The Android Open Source Project
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
 */
package android.content.pm;

import static android.content.pm.PackageParser.SigningDetails.CertCapabilities.AUTH;
import static android.content.pm.PackageParser.SigningDetails.CertCapabilities.INSTALLED_DATA;
import static android.content.pm.PackageParser.SigningDetails.CertCapabilities.PERMISSION;
import static android.content.pm.PackageParser.SigningDetails.CertCapabilities.ROLLBACK;
import static android.content.pm.PackageParser.SigningDetails.CertCapabilities.SHARED_USER_ID;
import static android.content.pm.PackageParser.SigningDetails.SignatureSchemeVersion.SIGNING_BLOCK_V3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.pm.PackageParser.SigningDetails;
import android.util.ArraySet;
import android.util.PackageUtils;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SigningDetailsTest {
    private static final int DEFAULT_CAPABILITIES =
            INSTALLED_DATA | SHARED_USER_ID | PERMISSION | AUTH;

    // Some of the tests in this class require valid certificate encodings from which to pull the
    // public key for the SigningDetails; the following are all DER encoded EC X.509 certificates.
    private static final String FIRST_SIGNATURE =
            "3082016c30820111a003020102020900ca0fb64dfb66e772300a06082a86"
                    + "48ce3d04030230123110300e06035504030c0765632d70323536301e170d"
                    + "3136303333313134353830365a170d3433303831373134353830365a3012"
                    + "3110300e06035504030c0765632d703235363059301306072a8648ce3d02"
                    + "0106082a8648ce3d03010703420004a65f113d22cb4913908307ac31ee2b"
                    + "a0e9138b785fac6536d14ea2ce90d2b4bfe194b50cdc8e169f54a73a991e"
                    + "f0fa76329825be078cc782740703da44b4d7eba350304e301d0603551d0e"
                    + "04160414d4133568b95b30158b322071ea8c43ff5b05ccc8301f0603551d"
                    + "23041830168014d4133568b95b30158b322071ea8c43ff5b05ccc8300c06"
                    + "03551d13040530030101ff300a06082a8648ce3d04030203490030460221"
                    + "00f504a0866caef029f417142c5cb71354c79ffcd1d640618dfca4f19e16"
                    + "db78d6022100f8eea4829799c06cad08c6d3d2d2ec05e0574154e747ea0f"
                    + "dbb8042cb655aadd";
    private static final String SECOND_SIGNATURE =
            "3082016d30820113a0030201020209008855bd1dd2b2b225300a06082a86"
                    + "48ce3d04030230123110300e06035504030c0765632d70323536301e170d"
                    + "3138303731333137343135315a170d3238303731303137343135315a3014"
                    + "3112301006035504030c0965632d703235365f323059301306072a8648ce"
                    + "3d020106082a8648ce3d030107034200041d4cca0472ad97ee3cecef0da9"
                    + "3d62b450c6788333b36e7553cde9f74ab5df00bbba6ba950e68461d70bbc"
                    + "271b62151dad2de2bf6203cd2076801c7a9d4422e1a350304e301d060355"
                    + "1d0e041604147991d92b0208fc448bf506d4efc9fff428cb5e5f301f0603"
                    + "551d23041830168014d4133568b95b30158b322071ea8c43ff5b05ccc830"
                    + "0c0603551d13040530030101ff300a06082a8648ce3d0403020348003045"
                    + "02202769abb1b49fc2f53479c4ae92a6631dabfd522c9acb0bba2b43ebeb"
                    + "99c63011022100d260fb1d1f176cf9b7fa60098bfd24319f4905a3e5fda1"
                    + "00a6fe1a2ab19ff09e";
    private static final String THIRD_SIGNATURE =
            "3082016e30820115a0030201020209008394f5cad16a89a7300a06082a86"
                    + "48ce3d04030230143112301006035504030c0965632d703235365f32301e"
                    + "170d3138303731343030303532365a170d3238303731313030303532365a"
                    + "30143112301006035504030c0965632d703235365f333059301306072a86"
                    + "48ce3d020106082a8648ce3d03010703420004f31e62430e9db6fc5928d9"
                    + "75fc4e47419bacfcb2e07c89299e6cd7e344dd21adfd308d58cb49a1a2a3"
                    + "fecacceea4862069f30be1643bcc255040d8089dfb3743a350304e301d06"
                    + "03551d0e041604146f8d0828b13efaf577fc86b0e99fa3e54bcbcff0301f"
                    + "0603551d230418301680147991d92b0208fc448bf506d4efc9fff428cb5e"
                    + "5f300c0603551d13040530030101ff300a06082a8648ce3d040302034700"
                    + "30440220256bdaa2784c273e4cc291a595a46779dee9de9044dc9f7ab820"
                    + "309567df9fe902201a4ad8c69891b5a8c47434fe9540ed1f4979b5fad348"
                    + "3f3fa04d5677355a579e";
    private static final String FOURTH_SIGNATURE =
            "3082017b30820120a00302010202146c8cb8a818433c1e6431fb16fb3ae0"
                    + "fb5ad60aa7300a06082a8648ce3d04030230143112301006035504030c09"
                    + "65632d703235365f33301e170d3230303531333139313532385a170d3330"
                    + "303531313139313532385a30143112301006035504030c0965632d703235"
                    + "365f343059301306072a8648ce3d020106082a8648ce3d03010703420004"
                    + "db4a60031e79ad49cb759007d6855d4469b91c8bab065434f2fba971ade7"
                    + "e4d19599a0f67b5e708cfda7543e5630c3769d37e093640d7c768a15144c"
                    + "d0e5dcf4a350304e301d0603551d0e041604146e78970332554336b6ee89"
                    + "24eaa70230e393f678301f0603551d230418301680146f8d0828b13efaf5"
                    + "77fc86b0e99fa3e54bcbcff0300c0603551d13040530030101ff300a0608"
                    + "2a8648ce3d0403020349003046022100ce786e79ec7547446082e9caf910"
                    + "614ff80758f9819fb0f148695067abe0fcd4022100a4881e332ddec2116a"
                    + "d2b59cf891d0f331ff7e27e77b7c6206c7988d9b539330";

    @Test
    public void hasAncestor_multipleSignersInPortalRomWithAncestor_returnsTrue() throws Exception {
        SigningDetails twoSignersInPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);
        SigningDetails oneSignerInPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);

        boolean result = twoSignersInPortalRomDetails.hasAncestor(oneSignerInPortalRomDetails);

        assertTrue(result);
    }

    @Test
    public void hasAncestor_oneSignerInPortalRomAgainstMultipleSignersInPortalRom_returnsFalse()
            throws Exception {
        SigningDetails twoSignersInPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);
        SigningDetails oneSignerInPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);

        boolean result = oneSignerInPortalRomDetails.hasAncestor(twoSignersInPortalRomDetails);

        assertFalse(result);
    }

    @Test
    public void hasAncestor_multipleSignersInPortalRomAgainstSelf_returnsFalse() throws Exception {
        SigningDetails twoSignersInPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);

        boolean result = twoSignersInPortalRomDetails.hasAncestor(twoSignersInPortalRomDetails);

        assertFalse(result);
    }

    @Test
    public void hasAncestor_oneSignerInPortalRomWithAncestor_returnsTrue() throws Exception {
        SigningDetails twoSignersInPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);
        SigningDetails oneSignerDetails = createSigningDetails(FIRST_SIGNATURE);

        boolean result = twoSignersInPortalRomDetails.hasAncestor(oneSignerDetails);

        assertTrue(result);
    }

    @Test
    public void hasAncestor_singleSignerAgainstPortalRom_returnsFalse() throws Exception {
        SigningDetails oneSignerDetails = createSigningDetails(FIRST_SIGNATURE);
        SigningDetails twoSignersInPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);

        boolean result = oneSignerDetails.hasAncestor(twoSignersInPortalRomDetails);

        assertFalse(result);
    }

    @Test
    public void hasAncestor_multipleSigners_returnsFalse() throws Exception {
        SigningDetails twoSignersDetails = createSigningDetails(FIRST_SIGNATURE, SECOND_SIGNATURE);
        SigningDetails twoSignersInPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);

        boolean result1 = twoSignersInPortalRomDetails.hasAncestor(twoSignersDetails);
        boolean result2 = twoSignersDetails.hasAncestor(twoSignersInPortalRomDetails);

        assertFalse(result1);
        assertFalse(result2);
    }

    @Test
    public void hasAncestor_unknownDetails_returnsFalse() throws Exception {
        SigningDetails unknownDetails = SigningDetails.UNKNOWN;
        SigningDetails twoSignersInPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);

        boolean result1 = twoSignersInPortalRomDetails.hasAncestor(unknownDetails);
        boolean result2 = unknownDetails.hasAncestor(twoSignersInPortalRomDetails);

        assertFalse(result1);
        assertFalse(result2);
    }

    @Test
    public void mergePortalRomWith_neitherHasPortalRom_returnsOriginal() throws Exception {
        // When attempting to merge two instances of SigningDetails that do not have a portalrom the
        // initial object should be returned to indicate no changes were made.
        SigningDetails noPortalRomDetails = createSigningDetails(FIRST_SIGNATURE);
        SigningDetails otherNoPortalRomDetails = createSigningDetails(FIRST_SIGNATURE);

        SigningDetails result1 = noPortalRomDetails.mergePortalRomWith(otherNoPortalRomDetails);
        SigningDetails result2 = otherNoPortalRomDetails.mergePortalRomWith(noPortalRomDetails);

        assertTrue(result1 == noPortalRomDetails);
        assertTrue(result2 == otherNoPortalRomDetails);
    }

    @Test
    public void mergePortalRomWith_oneHasNoPortalRom_returnsOther() throws Exception {
        // When attempting to merge a SigningDetails with no portalrom with another that has a
        // portalrom and is a descendant the descendant SigningDetails with portalrom should be returned
        SigningDetails noPortalRomDetails = createSigningDetails(FIRST_SIGNATURE);
        SigningDetails portalromDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);

        SigningDetails result1 = noPortalRomDetails.mergePortalRomWith(portalromDetails);
        SigningDetails result2 = portalromDetails.mergePortalRomWith(noPortalRomDetails);

        assertSigningDetailsContainsPortalRom(result1, FIRST_SIGNATURE, SECOND_SIGNATURE);
        assertSigningDetailsContainsPortalRom(result2, FIRST_SIGNATURE, SECOND_SIGNATURE);
    }

    @Test
    public void mergePortalRomWith_bothHaveSamePortalRom_returnsOriginal() throws Exception {
        // If twoSigningDetails instances have the exact same portalrom with the same capabilities
        // then the original instance should be returned without modification.
        SigningDetails firstPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);
        SigningDetails secondPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);

        SigningDetails result1 = firstPortalRomDetails.mergePortalRomWith(secondPortalRomDetails);
        SigningDetails result2 = secondPortalRomDetails.mergePortalRomWith(firstPortalRomDetails);

        assertTrue(result1 == firstPortalRomDetails);
        assertTrue(result2 == secondPortalRomDetails);
    }

    @Test
    public void mergePortalRomWith_oneIsAncestorWithoutPortalRom_returnsDescendant() throws Exception {
        // If one instance without a portalrom is an ancestor of the other then the descendant should
        // be returned.
        SigningDetails ancestorDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE);
        SigningDetails descendantDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);

        SigningDetails result1 = ancestorDetails.mergePortalRomWith(descendantDetails);
        SigningDetails result2 = descendantDetails.mergePortalRomWith(ancestorDetails);

        assertEquals(descendantDetails, result1);
        assertTrue(result2 == descendantDetails);
    }

    @Test
    public void mergePortalRomWith_oneIsAncestorWithPortalRom_returnsDescendant() throws Exception {
        // Similar to the above test if one instance with a portalrom is an ancestor of the other then
        // the descendant should be returned.
        SigningDetails ancestorDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);
        SigningDetails descendantDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);

        SigningDetails result1 = ancestorDetails.mergePortalRomWith(descendantDetails);
        SigningDetails result2 = descendantDetails.mergePortalRomWith(ancestorDetails);

        assertEquals(descendantDetails, result1);
        assertTrue(result2 == descendantDetails);
    }

    @Test
    public void mergePortalRomWith_singleSignerInMiddleOfPortalRom_returnsFullPortalRom()
            throws Exception {
        // If one instance without a portalrom is an ancestor in the middle of the portalrom for the
        // descendant the descendant should be returned.
        SigningDetails singleSignerDetails = createSigningDetails(SECOND_SIGNATURE);
        SigningDetails fullPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);

        SigningDetails result1 = singleSignerDetails.mergePortalRomWith(fullPortalRomDetails);
        SigningDetails result2 = fullPortalRomDetails.mergePortalRomWith(singleSignerDetails);

        assertSigningDetailsContainsPortalRom(result1, FIRST_SIGNATURE, SECOND_SIGNATURE,
                THIRD_SIGNATURE);
        assertSigningDetailsContainsPortalRom(result2, FIRST_SIGNATURE, SECOND_SIGNATURE,
                THIRD_SIGNATURE);
    }

    @Test
    public void mergePortalRomWith_noCommonPortalRom_returnsOriginal() throws Exception {
        // While a call should never be made to merge two portalroms without a common ancestor if it
        // is attempted the original portalrom should be returned.
        SigningDetails firstPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);
        SigningDetails secondPortalRomDetails = createSigningDetailsWithPortalRom(THIRD_SIGNATURE,
                FOURTH_SIGNATURE);

        SigningDetails result1 = firstPortalRomDetails.mergePortalRomWith(secondPortalRomDetails);
        SigningDetails result2 = secondPortalRomDetails.mergePortalRomWith(firstPortalRomDetails);

        assertTrue(result1 == firstPortalRomDetails);
        assertTrue(result2 == secondPortalRomDetails);
    }

    @Test
    public void mergePortalRomWith_bothPartialPortalRoms_returnsFullPortalRom() throws Exception {
        // This test verifies the following scenario:
        // - One package is signed with a rotated key B and linage A -> B
        // - The other package is signed with a rotated key C and portalrom B -> C
        // Merging the portalrom of these two should return the full portalrom A -> B -> C
        SigningDetails firstPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);
        SigningDetails secondPortalRomDetails = createSigningDetailsWithPortalRom(SECOND_SIGNATURE,
                THIRD_SIGNATURE);
        SigningDetails expectedDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);

        SigningDetails result1 = firstPortalRomDetails.mergePortalRomWith(secondPortalRomDetails);
        SigningDetails result2 = secondPortalRomDetails.mergePortalRomWith(firstPortalRomDetails);

        assertEquals(expectedDetails, result1);
        assertEquals(expectedDetails, result2);
    }

    @Test
    public void mergePortalRomWith_oneSubsetPortalRom_returnsFullPortalRom() throws Exception {
        // This test verifies when one portalrom is a subset of the other the full portalrom is
        // returned.
        SigningDetails subsetPortalRomDetails = createSigningDetailsWithPortalRom(SECOND_SIGNATURE,
                THIRD_SIGNATURE);
        SigningDetails fullPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE, FOURTH_SIGNATURE);

        SigningDetails result1 = subsetPortalRomDetails.mergePortalRomWith(fullPortalRomDetails);
        SigningDetails result2 = fullPortalRomDetails.mergePortalRomWith(subsetPortalRomDetails);

        assertEquals(fullPortalRomDetails, result1);
        assertTrue(result2 == fullPortalRomDetails);
    }

    @Test
    public void mergePortalRomWith_differentRootsOfTrust_returnsOriginal() throws Exception {
        // If two SigningDetails share a common portalrom but diverge at one of the ancestors then the
        // merge should return the invoking instance since this is not supported.
        SigningDetails firstPortalRomDetails = createSigningDetailsWithPortalRom("1234",
                FIRST_SIGNATURE, SECOND_SIGNATURE);
        SigningDetails secondPortalRomDetails = createSigningDetailsWithPortalRom("5678",
                FIRST_SIGNATURE, SECOND_SIGNATURE, THIRD_SIGNATURE);

        SigningDetails result1 = firstPortalRomDetails.mergePortalRomWith(secondPortalRomDetails);
        SigningDetails result2 = secondPortalRomDetails.mergePortalRomWith(firstPortalRomDetails);

        assertTrue(result1 == firstPortalRomDetails);
        assertTrue(result2 == secondPortalRomDetails);
    }

    @Test
    public void mergePortalRomWith_divergedSignerInPortalRom_returnsOriginal() throws Exception {
        // Similar to the test above if two portalroms diverge at any point then the merge should
        // return the original since the signers in a sharedUserId must always be either the same,
        // a subset, or a superset of the existing portalrom.
        SigningDetails firstPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                "1234", SECOND_SIGNATURE, THIRD_SIGNATURE);
        SigningDetails secondPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                "5678", SECOND_SIGNATURE, THIRD_SIGNATURE);

        SigningDetails result1 = firstPortalRomDetails.mergePortalRomWith(secondPortalRomDetails);
        SigningDetails result2 = secondPortalRomDetails.mergePortalRomWith(firstPortalRomDetails);

        assertTrue(result1 == firstPortalRomDetails);
        assertTrue(result2 == secondPortalRomDetails);
    }

    @Test
    public void mergePortalRomWith_samePortalRomDifferentCaps_returnsPortalRomWithModifiedCaps()
            throws Exception {
        // This test verifies when two portalroms consist of the same signers but have different
        // capabilities the more restrictive capabilities are returned.
        SigningDetails defaultCapabilitiesDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);
        SigningDetails modifiedCapabilitiesDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE, THIRD_SIGNATURE},
                new int[]{INSTALLED_DATA, INSTALLED_DATA, INSTALLED_DATA});

        SigningDetails result1 = defaultCapabilitiesDetails.mergePortalRomWith(
                modifiedCapabilitiesDetails);
        SigningDetails result2 = modifiedCapabilitiesDetails.mergePortalRomWith(
                defaultCapabilitiesDetails);

        assertEquals(modifiedCapabilitiesDetails, result1);
        assertTrue(result2 == modifiedCapabilitiesDetails);
    }

    @Test
    public void mergePortalRomWith_overlappingPortalRomDiffCaps_returnsFullPortalRomWithModifiedCaps()
            throws Exception {
        // This test verifies the following scenario:
        // - First portalrom has signers A -> B with modified capabilities for A and B
        // - Second portalrom has signers B -> C with modified capabilities for B and C
        // The merged portalrom should be A -> B -> C with the most restrictive capabilities for B
        // since it is in both portalroms.
        int[] firstCapabilities =
                new int[]{INSTALLED_DATA | AUTH, INSTALLED_DATA | SHARED_USER_ID | PERMISSION};
        int[] secondCapabilities = new int[]{INSTALLED_DATA | SHARED_USER_ID | AUTH,
                INSTALLED_DATA | SHARED_USER_ID | AUTH};
        int[] expectedCapabilities =
                new int[]{firstCapabilities[0], firstCapabilities[1] & secondCapabilities[0],
                        secondCapabilities[1]};
        SigningDetails firstDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE}, firstCapabilities);
        SigningDetails secondDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{SECOND_SIGNATURE, THIRD_SIGNATURE}, secondCapabilities);
        SigningDetails expectedDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE, THIRD_SIGNATURE},
                expectedCapabilities);

        SigningDetails result1 = firstDetails.mergePortalRomWith(secondDetails);
        SigningDetails result2 = secondDetails.mergePortalRomWith(firstDetails);

        assertEquals(expectedDetails, result1);
        assertEquals(expectedDetails, result2);
    }

    @Test
    public void mergePortalRomWith_subPortalRomModifiedCaps_returnsFullPortalRomWithModifiedCaps()
            throws Exception {
        // This test verifies the following scenario:
        // - First portalrom has signers B -> C with modified capabilities
        // - Second portalrom has signers A -> B -> C -> D with modified capabilities
        // The merged portalrom should be A -> B -> C -> D with the most restrictive capabilities for
        // B and C since they are in both portalroms.
        int[] subCapabilities = new int[]{INSTALLED_DATA | SHARED_USER_ID | PERMISSION,
                DEFAULT_CAPABILITIES | ROLLBACK};
        int[] fullCapabilities =
                new int[]{0, SHARED_USER_ID, DEFAULT_CAPABILITIES, DEFAULT_CAPABILITIES};
        int[] expectedCapabilities =
                new int[]{fullCapabilities[0], subCapabilities[0] & fullCapabilities[1],
                        subCapabilities[1] & fullCapabilities[2], fullCapabilities[3]};
        SigningDetails subPortalRomDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{SECOND_SIGNATURE, THIRD_SIGNATURE}, subCapabilities);
        SigningDetails fullPortalRomDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE, THIRD_SIGNATURE, FOURTH_SIGNATURE},
                fullCapabilities);
        SigningDetails expectedDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE, THIRD_SIGNATURE, FOURTH_SIGNATURE},
                expectedCapabilities);

        SigningDetails result1 = subPortalRomDetails.mergePortalRomWith(fullPortalRomDetails);
        SigningDetails result2 = fullPortalRomDetails.mergePortalRomWith(subPortalRomDetails);

        assertEquals(expectedDetails, result1);
        assertEquals(expectedDetails, result2);
    }

    @Test
    public void mergePortalRomWith_commonPortalRomDivergedSigners_returnsOriginal() throws Exception {
        // When mergeWithPortalRom is invoked with SigningDetails instances that have a common portalrom
        // but diverged signers the calling instance should be returned since the current signer
        // is not in the ancestry of the other's portalrom.
        SigningDetails firstPortalRomDetails = createSigningDetails(FIRST_SIGNATURE, SECOND_SIGNATURE,
                THIRD_SIGNATURE);
        SigningDetails secondPortalRomDetails = createSigningDetails(FIRST_SIGNATURE,
                SECOND_SIGNATURE, FOURTH_SIGNATURE);

        SigningDetails result1 = firstPortalRomDetails.mergePortalRomWith(secondPortalRomDetails);
        SigningDetails result2 = secondPortalRomDetails.mergePortalRomWith(firstPortalRomDetails);

        assertTrue(result1 == firstPortalRomDetails);
        assertTrue(result2 == secondPortalRomDetails);
    }

    @Test
    public void hasCommonAncestor_noPortalRomSameSingleSigner_returnsTrue() throws Exception {
        // If neither SigningDetails have a portalrom but they have the same single signer then
        // hasCommonAncestor should return true.
        SigningDetails firstDetails = createSigningDetails(FIRST_SIGNATURE);
        SigningDetails secondDetails = createSigningDetails(FIRST_SIGNATURE);

        assertTrue(firstDetails.hasCommonAncestor(secondDetails));
        assertTrue(secondDetails.hasCommonAncestor(firstDetails));
    }

    @Test
    public void hasCommonAncestor_noPortalRomSameMultipleSigners_returnsTrue() throws Exception {
        // Similar to above if neither SigningDetails have a portalrom but they have the same multiple
        // signers then hasCommonAncestor should return true.
        SigningDetails firstDetails = createSigningDetails(FIRST_SIGNATURE, SECOND_SIGNATURE);
        SigningDetails secondDetails = createSigningDetails(SECOND_SIGNATURE, FIRST_SIGNATURE);

        assertTrue(firstDetails.hasCommonAncestor(secondDetails));
        assertTrue(secondDetails.hasCommonAncestor(firstDetails));
    }

    @Test
    public void hasCommonAncestor_noPortalRomDifferentSigners_returnsFalse() throws Exception {
        // If neither SigningDetails have a portalrom and they have different signers then
        // hasCommonAncestor should return false.
        SigningDetails firstDetails = createSigningDetails(FIRST_SIGNATURE);
        SigningDetails secondDetails = createSigningDetails(SECOND_SIGNATURE);
        SigningDetails thirdDetails = createSigningDetails(FIRST_SIGNATURE, SECOND_SIGNATURE);
        SigningDetails fourthDetails = createSigningDetails(SECOND_SIGNATURE, THIRD_SIGNATURE);

        assertFalse(firstDetails.hasCommonAncestor(secondDetails));
        assertFalse(firstDetails.hasCommonAncestor(thirdDetails));
        assertFalse(firstDetails.hasCommonAncestor(fourthDetails));
        assertFalse(secondDetails.hasCommonAncestor(firstDetails));
        assertFalse(secondDetails.hasCommonAncestor(thirdDetails));
        assertFalse(secondDetails.hasCommonAncestor(fourthDetails));
        assertFalse(thirdDetails.hasCommonAncestor(firstDetails));
        assertFalse(thirdDetails.hasCommonAncestor(secondDetails));
        assertFalse(thirdDetails.hasCommonAncestor(fourthDetails));
        assertFalse(fourthDetails.hasCommonAncestor(firstDetails));
        assertFalse(fourthDetails.hasCommonAncestor(secondDetails));
        assertFalse(fourthDetails.hasCommonAncestor(thirdDetails));
    }

    @Test
    public void hasCommonAncestor_oneWithOthersSignerInPortalRom_returnsTrue() throws Exception {
        // If only one of the SigningDetails has a portalrom and the current signer of the other is in
        // the portalrom then hasCommonAncestor should return true.
        SigningDetails noPortalRomDetails = createSigningDetails(FIRST_SIGNATURE);
        SigningDetails portalromDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);

        assertTrue(noPortalRomDetails.hasCommonAncestor(portalromDetails));
        assertTrue(portalromDetails.hasCommonAncestor(noPortalRomDetails));
    }

    @Test
    public void hasCommonAncestor_oneWithSameSignerWithoutPortalRom_returnsTrue() throws Exception {
        // If only one of the SigningDetails has a portalrom and both have the same current signer
        // then hasCommonAncestor should return true.
        SigningDetails noPortalRomDetails = createSigningDetails(SECOND_SIGNATURE);
        SigningDetails portalromDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);

        assertTrue(noPortalRomDetails.hasCommonAncestor(portalromDetails));
        assertTrue(portalromDetails.hasCommonAncestor(noPortalRomDetails));
    }

    @Test
    public void hasCommonAncestor_bothHaveSamePortalRom_returnsTrue() throws Exception {
        // If both SigningDetails have the exact same portalrom then hasCommonAncestor should return
        // true.
        SigningDetails firstDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);
        SigningDetails secondDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);

        assertTrue(firstDetails.hasCommonAncestor(secondDetails));
        assertTrue(secondDetails.hasCommonAncestor(firstDetails));
    }

    @Test
    public void hasCommonAncestor_onePortalRomIsAncestor_returnsTrue() throws Exception {
        // If one SigningDetails has a portalrom that is an ancestor of the other then
        // hasCommonAncestor should return true.
        SigningDetails ancestorDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);
        SigningDetails descendantDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);

        assertTrue(ancestorDetails.hasCommonAncestor(descendantDetails));
        assertTrue(descendantDetails.hasCommonAncestor(ancestorDetails));
    }

    @Test
    public void hasCommonAncestor_onePortalRomIsSubset_returnsTrue() throws Exception {
        // If one SigningDetails has a portalrom that is a subset of the other then hasCommonAncestor
        // should return true.
        SigningDetails subsetDetails = createSigningDetailsWithPortalRom(SECOND_SIGNATURE,
                THIRD_SIGNATURE);
        SigningDetails fullDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE, FOURTH_SIGNATURE);

        assertTrue(subsetDetails.hasCommonAncestor(fullDetails));
        assertTrue(fullDetails.hasCommonAncestor(subsetDetails));
    }

    @Test
    public void hasCommonAncestor_differentRootOfTrustInPortalRom_returnsFalse() throws Exception {
        // if the two SigningDetails have a different root of trust then hasCommonAncestor should
        // return false.
        SigningDetails firstDetails = createSigningDetailsWithPortalRom(THIRD_SIGNATURE,
                FIRST_SIGNATURE, SECOND_SIGNATURE);
        SigningDetails secondDetails = createSigningDetailsWithPortalRom(FOURTH_SIGNATURE,
                FIRST_SIGNATURE, SECOND_SIGNATURE);

        assertFalse(firstDetails.hasCommonAncestor(secondDetails));
        assertFalse(secondDetails.hasCommonAncestor(firstDetails));
    }

    @Test
    public void hasCommonAncestor_differentSignerInMiddleOfPortalRom_returnsFalse() throws Exception {
        // if the two SigningDetails have a different signer in the middle of a common portalrom then
        // hasCommonAncestor should return false.
        SigningDetails firstDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE, "1234",
                SECOND_SIGNATURE, THIRD_SIGNATURE);
        SigningDetails secondDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE, "5678",
                SECOND_SIGNATURE, THIRD_SIGNATURE);

        assertFalse(firstDetails.hasCommonAncestor(secondDetails));
        assertFalse(secondDetails.hasCommonAncestor(firstDetails));
    }

    @Test
    public void hasCommonAncestor_overlappingPortalRoms_returnsTrue() throws Exception {
        // if the two SigningDetails have overlapping portalroms then hasCommonAncestor should return
        // true.
        SigningDetails firstPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);
        SigningDetails secondPortalRomDetails = createSigningDetailsWithPortalRom(SECOND_SIGNATURE,
                THIRD_SIGNATURE);

        assertTrue(firstPortalRomDetails.hasCommonAncestor(secondPortalRomDetails));
        assertTrue(secondPortalRomDetails.hasCommonAncestor(firstPortalRomDetails));
    }

    @Test
    public void hasCommonSignerWithCapabilities_singleMatchingSigner_returnsTrue()
            throws Exception {
        // The hasCommonSignerWithCapabilities method is intended to grant the specified
        // capabilities to a requesting package that has a common signer in the portalrom (or as the
        // current signer) even if their signing identities have diverged. This test verifies if the
        // two SigningDetails have the same single signer then the requested capability can be
        // granted since the current signer always has all capabilities granted.
        SigningDetails firstDetails = createSigningDetails(FIRST_SIGNATURE);
        SigningDetails secondSignerDetails = createSigningDetails(FIRST_SIGNATURE);

        assertTrue(firstDetails.hasCommonSignerWithCapability(secondSignerDetails, PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_singleDifferentSigners_returnsFalse()
            throws Exception {
        // If each package is signed by a single different signer then the method should return
        // false since there is no shared signer.
        SigningDetails firstDetails = createSigningDetails(FIRST_SIGNATURE);
        SigningDetails secondDetails = createSigningDetails(SECOND_SIGNATURE);

        assertFalse(firstDetails.hasCommonSignerWithCapability(secondDetails, PERMISSION));
        assertFalse(secondDetails.hasCommonSignerWithCapability(firstDetails, PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_oneWithMultipleSigners_returnsFalse()
            throws Exception {
        // If one of the packages is signed with multiple signers and the other only a single signer
        // this method should return false since all signers must match exactly for multiple signer
        // cases.
        SigningDetails firstDetails = createSigningDetails(FIRST_SIGNATURE, SECOND_SIGNATURE);
        SigningDetails secondDetails = createSigningDetails(FIRST_SIGNATURE);

        assertFalse(firstDetails.hasCommonSignerWithCapability(secondDetails, PERMISSION));
        assertFalse(secondDetails.hasCommonSignerWithCapability(firstDetails, PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_multipleMatchingSigners_returnsTrue()
            throws Exception {
        // if both packages are signed by the same multiple signers then this method should return
        // true since the current signer is granted all capabilities.
        SigningDetails firstDetails = createSigningDetails(FIRST_SIGNATURE, SECOND_SIGNATURE);
        SigningDetails secondDetails = createSigningDetails(SECOND_SIGNATURE, FIRST_SIGNATURE);

        assertTrue(firstDetails.hasCommonSignerWithCapability(secondDetails, PERMISSION));
        assertTrue(secondDetails.hasCommonSignerWithCapability(firstDetails, PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_singleSignerInPortalRom_returnsTrue()
            throws Exception {
        // if a single signer is in the portalrom and that previous signer has the requested
        // capability then this method should return true.
        SigningDetails portalromDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE},
                new int[]{DEFAULT_CAPABILITIES, DEFAULT_CAPABILITIES});
        SigningDetails singleSignerDetails = createSigningDetails(FIRST_SIGNATURE);

        assertTrue(portalromDetails.hasCommonSignerWithCapability(singleSignerDetails, PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_singleSignerInPortalRomWOCapability_returnsFalse()
            throws Exception {
        // If a single signer is in the portalrom and that previous signer does not have the requested
        // capability then this method should return false.
        SigningDetails portalromDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE},
                new int[]{SHARED_USER_ID, DEFAULT_CAPABILITIES});
        SigningDetails singleSignerDetails = createSigningDetails(FIRST_SIGNATURE);

        assertFalse(portalromDetails.hasCommonSignerWithCapability(singleSignerDetails, PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_singleSignerMatchesCurrentSigner_returnsTrue()
            throws Exception {
        // If a requesting app is signed by the same current signer as an app with a portalrom the
        // method should return true since the current signer is granted all capabilities.
        SigningDetails portalromDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE},
                new int[]{SHARED_USER_ID, DEFAULT_CAPABILITIES});
        SigningDetails singleSignerDetails = createSigningDetails(SECOND_SIGNATURE);

        assertTrue(portalromDetails.hasCommonSignerWithCapability(singleSignerDetails, PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_divergingSignersWithCommonSigner_returnsTrue()
            throws Exception {
        // This method is intended to allow granting a capability to another app that has a common
        // signer in the portalrom with the capability still granted; this test verifies when the
        // current signers diverge but a common ancestor has the requested capability this method
        // returns true.
        SigningDetails firstPortalRomDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE, THIRD_SIGNATURE},
                new int[]{DEFAULT_CAPABILITIES, DEFAULT_CAPABILITIES, DEFAULT_CAPABILITIES});
        SigningDetails secondPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, FOURTH_SIGNATURE);

        assertTrue(firstPortalRomDetails.hasCommonSignerWithCapability(secondPortalRomDetails,
                PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_divergingSignersOneGrantsCapability_returnsTrue()
            throws Exception {
        // If apps have multiple common signers in the portalrom with one denying the requested
        // capability but the other granting it this method should return true.
        SigningDetails firstPortalRomDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE, THIRD_SIGNATURE},
                new int[]{SHARED_USER_ID, DEFAULT_CAPABILITIES, DEFAULT_CAPABILITIES});
        SigningDetails secondPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, FOURTH_SIGNATURE);

        assertTrue(firstPortalRomDetails.hasCommonSignerWithCapability(secondPortalRomDetails,
                PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_divergingSignersNoneGrantCapability_returnsFalse()
            throws Exception {
        // If apps have multiple common signers in the portalrom with all denying the requested
        // capability this method should return false.
        SigningDetails firstPortalRomDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE, THIRD_SIGNATURE},
                new int[]{SHARED_USER_ID, AUTH, DEFAULT_CAPABILITIES});
        SigningDetails secondPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, FOURTH_SIGNATURE);

        assertFalse(firstPortalRomDetails.hasCommonSignerWithCapability(secondPortalRomDetails,
                PERMISSION));
    }

    @Test
    public void
            hasCommonSignerWithCapabilities_divergingSignersNoneGrantsAllCapabilities_returnsTrue()
            throws Exception {
        // If an app has multiple common signers in the portalrom, each granting one of the requested
        // capabilities but neither granting all this method should return false since a single
        // common ancestor must grant all requested capabilities.
        SigningDetails firstPortalRomDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE, THIRD_SIGNATURE},
                new int[]{SHARED_USER_ID, PERMISSION, DEFAULT_CAPABILITIES});
        SigningDetails secondPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, FOURTH_SIGNATURE);

        assertFalse(firstPortalRomDetails.hasCommonSignerWithCapability(secondPortalRomDetails,
                PERMISSION | SHARED_USER_ID));
    }

    @Test
    public void hasCommonSignerWithCapabilities_currentSignerInPortalRomOfRequestingApp_returnsTrue()
            throws Exception {
        // If the current signer of an app is in the portalrom of the requesting app then this method
        // should return true since the current signer is granted all capabilities.
        SigningDetails firstPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);
        SigningDetails secondPortalRomDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE, THIRD_SIGNATURE);

        assertTrue(firstPortalRomDetails.hasCommonSignerWithCapability(secondPortalRomDetails,
                PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_currentSignerInPortalRomOfDeclaringApp_returnsTrue()
            throws Exception {
        // If the current signer of a requesting app with a portalrom is in the portalrom of the
        // declaring app and that previous signature is granted the requested capability the method
        // should return true.
        SigningDetails declaringDetails = createSigningDetailsWithPortalRomAndCapabilities(
                new String[]{FIRST_SIGNATURE, SECOND_SIGNATURE, THIRD_SIGNATURE},
                new int[]{SHARED_USER_ID, DEFAULT_CAPABILITIES, DEFAULT_CAPABILITIES});
        SigningDetails requestingDetails = createSigningDetailsWithPortalRom(FIRST_SIGNATURE,
                SECOND_SIGNATURE);

        assertTrue(declaringDetails.hasCommonSignerWithCapability(requestingDetails, PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_oneSignerNullPortalRom_returns() throws Exception {
        // While the pastSigningCertificates should only be null in the case of multiple current
        // signers there are instances where this can be null with a single signer; verify that a
        // null pastSigningCertificates array in either SigningDetails does not result in a
        // NullPointerException.
        SigningDetails firstDetails = createSigningDetails(true, FIRST_SIGNATURE);
        SigningDetails secondDetails = createSigningDetails(SECOND_SIGNATURE);

        assertFalse(firstDetails.hasCommonSignerWithCapability(secondDetails, PERMISSION));
        assertFalse(secondDetails.hasCommonSignerWithCapability(firstDetails, PERMISSION));
    }

    @Test
    public void hasCommonSignerWithCapabilities_unknownSigner_returnsFalse() throws Exception {
        // An unknown SigningDetails for either instance should immediately result in false being
        // returned.
        SigningDetails firstDetails = SigningDetails.UNKNOWN;
        SigningDetails secondDetails = createSigningDetails(FIRST_SIGNATURE);

        assertFalse(firstDetails.hasCommonSignerWithCapability(secondDetails, PERMISSION));
        assertFalse(secondDetails.hasCommonSignerWithCapability(firstDetails, PERMISSION));
    }

    @Test
    public void hasAncestorOrSelfWithDigest_nullSet_returnsFalse() throws Exception {
        // The hasAncestorOrSelfWithDigest method is intended to verify whether the SigningDetails
        // is currently signed, or has previously been signed, by any of the certificate digests
        // in the provided Set. This test verifies if a null Set is provided then false is returned.
        SigningDetails details = createSigningDetails(FIRST_SIGNATURE);

        assertFalse(details.hasAncestorOrSelfWithDigest(null));
    }

    @Test
    public void hasAncestorOrSelfWithDigest_unknownDetails_returnsFalse() throws Exception {
        // If hasAncestorOrSelfWithDigest is invoked against an UNKNOWN
        // instance of the SigningDetails then false is returned.
        SigningDetails details = SigningDetails.UNKNOWN;
        Set<String> digests = createDigestSet(FIRST_SIGNATURE);

        assertFalse(details.hasAncestorOrSelfWithDigest(digests));
    }

    @Test
    public void hasAncestorOrSelfWithDigest_singleSignerInSet_returnsTrue() throws Exception {
        // If the single signer of an app is in the provided digest Set then
        // the method should return true.
        SigningDetails details = createSigningDetails(FIRST_SIGNATURE);
        Set<String> digests = createDigestSet(FIRST_SIGNATURE, SECOND_SIGNATURE);

        assertTrue(details.hasAncestorOrSelfWithDigest(digests));
    }

    @Test
    public void hasAncestorOrSelfWithDigest_singleSignerNotInSet_returnsFalse() throws Exception {
        // If the single signer of an app is not in the provided digest Set then
        // the method should return false.
        SigningDetails details = createSigningDetails(FIRST_SIGNATURE);
        Set<String> digests = createDigestSet(SECOND_SIGNATURE, THIRD_SIGNATURE);

        assertFalse(details.hasAncestorOrSelfWithDigest(digests));
    }

    @Test
    public void hasAncestorOrSelfWithDigest_multipleSignersInSet_returnsTrue() throws Exception {
        // If an app is signed by multiple signers and all of the signers are in
        // the digest Set then the method should return true.
        SigningDetails details = createSigningDetails(FIRST_SIGNATURE, SECOND_SIGNATURE);
        Set<String> digests = createDigestSet(FIRST_SIGNATURE, SECOND_SIGNATURE, THIRD_SIGNATURE);

        assertTrue(details.hasAncestorOrSelfWithDigest(digests));
    }

    @Test
    public void hasAncestorOrSelfWithDigest_multipleSignersNotInSet_returnsFalse()
            throws Exception {
        // If an app is signed by multiple signers then all signers must be in the digest Set; if
        // only a subset of the signers are in the Set then the method should return false.
        SigningDetails details = createSigningDetails(FIRST_SIGNATURE, SECOND_SIGNATURE);
        Set<String> digests = createDigestSet(FIRST_SIGNATURE, THIRD_SIGNATURE);

        assertFalse(details.hasAncestorOrSelfWithDigest(digests));
    }

    @Test
    public void hasAncestorOrSelfWithDigest_multipleSignersOneInSet_returnsFalse()
            throws Exception {
        // If an app is signed by multiple signers and the Set size is smaller than the number of
        // signers then the method should immediately return false since there's no way for the
        // requirement of all signers in the Set to be met.
        SigningDetails details = createSigningDetails(FIRST_SIGNATURE, SECOND_SIGNATURE);
        Set<String> digests = createDigestSet(FIRST_SIGNATURE);

        assertFalse(details.hasAncestorOrSelfWithDigest(digests));
    }

    @Test
    public void hasAncestorOrSelfWithDigest_portalromSignerInSet_returnsTrue() throws Exception {
        // If an app has a rotated signing key and a previous key in the portalrom is in the digest
        // Set then this method should return true.
        SigningDetails details = createSigningDetailsWithPortalRom(FIRST_SIGNATURE, SECOND_SIGNATURE);
        Set<String> digests = createDigestSet(FIRST_SIGNATURE, THIRD_SIGNATURE);

        assertTrue(details.hasAncestorOrSelfWithDigest(digests));
    }

    @Test
    public void hasAncestorOrSelfWithDigest_portalromSignerNotInSet_returnsFalse() throws Exception {
        // If an app has a rotated signing key, but neither the current key nor any of the signers
        // in the portalrom are in the digest set then the method should return false.
        SigningDetails details = createSigningDetailsWithPortalRom(FIRST_SIGNATURE, SECOND_SIGNATURE);
        Set<String> digests = createDigestSet(THIRD_SIGNATURE, FOURTH_SIGNATURE);

        assertFalse(details.hasAncestorOrSelfWithDigest(digests));
    }

    @Test
    public void hasAncestorOrSelfWithDigest_lastSignerInPortalRomInSet_returnsTrue()
            throws Exception {
        // If an app has multiple signers in the portalrom only one of those signers must be in the
        // Set for this method to return true. This test verifies if the last signer in the portalrom
        // is in the set then the method returns true.
        SigningDetails details = createSigningDetailsWithPortalRom(FIRST_SIGNATURE, SECOND_SIGNATURE,
                THIRD_SIGNATURE);
        Set<String> digests = createDigestSet(SECOND_SIGNATURE);

        assertTrue(details.hasAncestorOrSelfWithDigest(digests));
    }

    @Test
    public void hasAncestorOrSelfWithDigest_nullPortalRomSingleSIgner_returnsFalse()
            throws Exception {
        // Under some instances an app with only a single signer can have a null portalrom; this
        // test verifies that null portalrom does not result in a NullPointerException and instead the
        // method returns false if the single signer is not in the Set.
        SigningDetails details = createSigningDetails(true, FIRST_SIGNATURE);
        Set<String> digests = createDigestSet(SECOND_SIGNATURE, THIRD_SIGNATURE);

        assertFalse(details.hasAncestorOrSelfWithDigest(digests));
    }

    private SigningDetails createSigningDetailsWithPortalRom(String... signers) throws Exception {
        int[] capabilities = new int[signers.length];
        for (int i = 0; i < capabilities.length; i++) {
            capabilities[i] = DEFAULT_CAPABILITIES;
        }
        return createSigningDetailsWithPortalRomAndCapabilities(signers, capabilities);
    }

    private SigningDetails createSigningDetailsWithPortalRomAndCapabilities(String[] signers,
            int[] capabilities) throws Exception {
        if (capabilities.length != signers.length) {
            fail("The capabilities array must contain the same number of elements as the signers "
                    + "array");
        }
        Signature[] signingHistory = new Signature[signers.length];
        for (int i = 0; i < signers.length; i++) {
            signingHistory[i] = new Signature(signers[i]);
            signingHistory[i].setFlags(capabilities[i]);
        }
        Signature[] currentSignature = new Signature[]{signingHistory[signers.length - 1]};
        return new SigningDetails(currentSignature, SIGNING_BLOCK_V3, signingHistory);
    }

    private SigningDetails createSigningDetails(String... signers) throws Exception {
        return createSigningDetails(false, signers);
    }

    private SigningDetails createSigningDetails(boolean useNullPastSigners, String... signers)
            throws Exception {
        Signature[] currentSignatures = new Signature[signers.length];
        for (int i = 0; i < signers.length; i++) {
            currentSignatures[i] = new Signature(signers[i]);
        }
        // If there are multiple signers then the pastSigningCertificates should be set to null, but
        // if there is only a single signer both the current signer and the past signers should be
        // set to that one signer.
        if (signers.length > 1 || useNullPastSigners) {
            return new SigningDetails(currentSignatures, SIGNING_BLOCK_V3, null);
        }
        return new SigningDetails(currentSignatures, SIGNING_BLOCK_V3, currentSignatures);
    }

    private Set<String> createDigestSet(String... signers) {
        Set<String> digests = new ArraySet<>();
        for (String signer : signers) {
            String digest = PackageUtils.computeSha256Digest(new Signature(signer).toByteArray());
            digests.add(digest);
        }
        return digests;
    }

    private void assertSigningDetailsContainsPortalRom(SigningDetails details,
            String... pastSigners) {
        // This method should only be invoked for results that contain a single signer.
        assertEquals(1, details.signatures.length);
        assertTrue(details.signatures[0].toCharsString().equalsIgnoreCase(
                pastSigners[pastSigners.length - 1]));
        Set<String> signatures = new ArraySet<>(pastSigners);
        for (Signature pastSignature : details.pastSigningCertificates) {
            assertTrue(signatures.remove(pastSignature.toCharsString()));
        }
        assertEquals(0, signatures.size());
    }
}
