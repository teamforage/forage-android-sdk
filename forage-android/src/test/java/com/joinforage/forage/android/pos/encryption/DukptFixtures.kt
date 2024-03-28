package com.joinforage.forage.android.pos.encryption

import com.joinforage.forage.android.pos.encryption.dukpt.DukptService
import com.joinforage.forage.android.pos.encryption.dukpt.KsnComponent
import com.joinforage.forage.android.pos.encryption.storage.InMemoryKeyRegisters
import com.joinforage.forage.android.pos.encryption.storage.KeySerialNumber

internal object DukptFixtures {
    fun newDukpt(): Triple<DukptService, InMemoryKeyRegisters, KeySerialNumber> {
        val keyRegisters = InMemoryKeyRegisters()
        val dukpt = DukptService(
            ksn = KeySerialNumber(Config.InitialKeyId),
            keyRegisters = keyRegisters
        )
        val nextKsn = dukpt.loadKey(Config.InitialDerivationKeyMaterial)
        return Triple(dukpt, keyRegisters, nextKsn)
    }

    object Config {
        // KSN = DerivationID | Base Derivation Key ID | TxCounter
        val BaseDerivationKeyId = KsnComponent(
            byteArrayOf(
                0x12.toByte(),
                0x34.toByte(),
                0x56.toByte(),
                0x78.toByte()
            )
        )
        val DerivationDeviceId = KsnComponent(
            byteArrayOf(
                0x90.toByte(),
                0x12.toByte(),
                0x34.toByte(),
                0x56.toByte()
            )
        )
        val InitialKeyId = "${BaseDerivationKeyId.toHexString()}${DerivationDeviceId.toHexString()}"

        val InitialDerivationKeyMaterial = AesBlock(
            byteArrayOf(
                0x12.toByte(), 0x73.toByte(), 0x67.toByte(), 0x1e.toByte(),
                0xa2.toByte(), 0x6a.toByte(), 0xc2.toByte(), 0x9a.toByte(),
                0xfa.toByte(), 0x4d.toByte(), 0x10.toByte(), 0x84.toByte(),
                0x12.toByte(), 0x76.toByte(), 0x52.toByte(), 0xa1.toByte()
            )
        )
    }

    object IntermediateKeys {
        val AfterLoadKey = listOf(
            "4f21b565bad9835e112b6465635eae44", // 0
            "2f34d68de10f68d38091a73b9e7c437c",
            "0eefc7ada628ba68878da9165a8a1887",
            "718ee6cf0b27e53d5f7af99c4d8146a2",
            "7459762eed7f51d08567ed6598dfbea2",
            "1ed39390b4448c69819eb55f4c616564",
            "c13bda0a56d6998e544e0a10a3d979da",
            "089f6b989ca13d49a6a0317f85460ce5",
            "065355a6a3dd4c2260bdddfa0c16704e",
            "cf16febc5cfd1a741a3280564a9681f2",
            "4bf8eb1daf9f4244332ed01663eb654e",
            "492248fee0fe87e8b5db7bb2ac7bc955",
            "18690547eb19d28efaf5ef6d22c271aa",
            "84f4cca45c4f1d4e063f1ce5b95b6c7f",
            "4ec5fc0c3cd62aff174a37b6fdc2b0d9",
            "9ef99a4d5fd548a23d299074047f7f6b",
            "f4c6237db49e28bf96e6a18cd8cdda00",
            "f7ae9025468a25d37b7249cffed224c8",
            "579594a986e87917382a181576fa7a9a",
            "5aaf46aad7593e0d224e05e13629ed1e",
            "5787eb837b6ffb3af24759f8625cec19",
            "988a3ab89b9332a15d0be2c54c279923",
            "e55171636976bdc5758a6fa4c25f0008",
            "df16d5bac52ffa7564d7dbd2de7c6ccf",
            "145e8c933fc0d61900592035cf18a5af",
            "fbdf917e209b42f9db8843d18bee8033",
            "61c70779f920bbd37815c21b5a1a7b75",
            "97d7bb3fc342b9e961308bb8b801775b",
            "69b453118411404db54ae2b751f02f43",
            "7cce4e679f4fc3478e3cd4509d64a7f3",
            "36af4aa9fc1100b2ae7742101540340a",
            "9dc56486499a2e857fdefc4740641ea8" // 31
        )

        val AfterTx1 = listOf(
            null,
            "2f34d68de10f68d38091a73b9e7c437c", // 0
            "0eefc7ada628ba68878da9165a8a1887",
            "718ee6cf0b27e53d5f7af99c4d8146a2",
            "7459762eed7f51d08567ed6598dfbea2",
            "1ed39390b4448c69819eb55f4c616564",
            "c13bda0a56d6998e544e0a10a3d979da",
            "089f6b989ca13d49a6a0317f85460ce5",
            "065355a6a3dd4c2260bdddfa0c16704e",
            "cf16febc5cfd1a741a3280564a9681f2",
            "4bf8eb1daf9f4244332ed01663eb654e",
            "492248fee0fe87e8b5db7bb2ac7bc955",
            "18690547eb19d28efaf5ef6d22c271aa",
            "84f4cca45c4f1d4e063f1ce5b95b6c7f",
            "4ec5fc0c3cd62aff174a37b6fdc2b0d9",
            "9ef99a4d5fd548a23d299074047f7f6b",
            "f4c6237db49e28bf96e6a18cd8cdda00",
            "f7ae9025468a25d37b7249cffed224c8",
            "579594a986e87917382a181576fa7a9a",
            "5aaf46aad7593e0d224e05e13629ed1e",
            "5787eb837b6ffb3af24759f8625cec19",
            "988a3ab89b9332a15d0be2c54c279923",
            "e55171636976bdc5758a6fa4c25f0008",
            "df16d5bac52ffa7564d7dbd2de7c6ccf",
            "145e8c933fc0d61900592035cf18a5af",
            "fbdf917e209b42f9db8843d18bee8033",
            "61c70779f920bbd37815c21b5a1a7b75",
            "97d7bb3fc342b9e961308bb8b801775b",
            "69b453118411404db54ae2b751f02f43",
            "7cce4e679f4fc3478e3cd4509d64a7f3",
            "36af4aa9fc1100b2ae7742101540340a",
            "9dc56486499a2e857fdefc4740641ea8" // 31
        )

        val AfterTx2 = listOf(
            "031504e530365cf81264238540518318", // 0
            null,
            "0eefc7ada628ba68878da9165a8a1887",
            "718ee6cf0b27e53d5f7af99c4d8146a2",
            "7459762eed7f51d08567ed6598dfbea2",
            "1ed39390b4448c69819eb55f4c616564",
            "c13bda0a56d6998e544e0a10a3d979da",
            "089f6b989ca13d49a6a0317f85460ce5",
            "065355a6a3dd4c2260bdddfa0c16704e",
            "cf16febc5cfd1a741a3280564a9681f2",
            "4bf8eb1daf9f4244332ed01663eb654e",
            "492248fee0fe87e8b5db7bb2ac7bc955",
            "18690547eb19d28efaf5ef6d22c271aa",
            "84f4cca45c4f1d4e063f1ce5b95b6c7f",
            "4ec5fc0c3cd62aff174a37b6fdc2b0d9",
            "9ef99a4d5fd548a23d299074047f7f6b",
            "f4c6237db49e28bf96e6a18cd8cdda00",
            "f7ae9025468a25d37b7249cffed224c8",
            "579594a986e87917382a181576fa7a9a",
            "5aaf46aad7593e0d224e05e13629ed1e",
            "5787eb837b6ffb3af24759f8625cec19",
            "988a3ab89b9332a15d0be2c54c279923",
            "e55171636976bdc5758a6fa4c25f0008",
            "df16d5bac52ffa7564d7dbd2de7c6ccf",
            "145e8c933fc0d61900592035cf18a5af",
            "fbdf917e209b42f9db8843d18bee8033",
            "61c70779f920bbd37815c21b5a1a7b75",
            "97d7bb3fc342b9e961308bb8b801775b",
            "69b453118411404db54ae2b751f02f43",
            "7cce4e679f4fc3478e3cd4509d64a7f3",
            "36af4aa9fc1100b2ae7742101540340a",
            "9dc56486499a2e857fdefc4740641ea8" // 31
        )
    }

    val WorkingKeys = listOf(
        "af8cb133a78f8dc2d1359f18527593fb",
        "d30bdc73ec9714b000bec66bdb7b6d09",
        "7d69f01f3b45449f62c7816ece723268",
        "91a0588318ec2673214271f70137896e",
        "35a43bc9efeb09c756204b57e3fb7d4d",
        "02dcc6cd1201a3a2ca7099559c862123",
        "6ecf912f3b18ca11a7a27bb60705fd09",
        "4d9df3fbee3448fc3e676d04320a90f5",
        "b4273d0f8804829572aff61fa2bc417f",
        "ba7cf1abf6b4a23a16a07cde63f3514c",
        "fbb09b064691293a3ffba322f2c55014",
        "6743311b691a3065561bb075eb39da2d",
        "0bac7e46f72c543f06c7144080e2ad4b",
        "3453aa079ff413d9ff51e6055f03b664",
        "4a22d5a8b6b6c06292eaac6a1353c369",
        "a09c63853b707708deab907ba778c191",
        "0c30f0d8d070b90ff25d83b6e23bc1a4",
        "50233141fe61ddaa8c444df112eace8e",
        "155fa01a41971c9a185e125f2d99586c",
        "fd8ecd02676402dd573468d66c64f898",
        "5ecc93425a8bb6615f3857d3163f33a2",
        "41c280039c32390b817f30902ea389bf",
        "b7eaf85c830839be9d5523947c5d7072",
        "d22c707d6464a197a9186ffc8a5b9fc4",
        "4e36edc3020cffb882dd88e9cf1e451f",
        "44d03fad6371683a042057e43522ffb2",
        "3e61011d8e421453bb78e22c6bca789b",
        "485a7ea20f71519af37df4a45d22af67",
        "840bc596f423dcf5748dfdf8166e33e6",
        "37dffa00c03799daf195493d89740c8b",
        "d42bf5d7fc7544826e4b3bba2a19f124",
        "d4dd318bb3d0185de4bf6886262669df",
        "82100faaa6847aab16d6a58e077c7449",
        "6969bd538f8d18f566fb62bea49bd0ab",
        "fd2714d0bcffa395532cf9b038554059",
        "b3a4942eb350381c1e669f65c24725eb",
        "e654a3c9b68dc4ba9eed65481bcf6af0",
        "7a0fc50eb24465f65aa1fd2c603a5a15",
        "607250fec965315794ef4ba711f460d7",
        "4ac58e91005102ccb9d1671cb726429f",
        "5ea73f7eb5f4632a9d7d59b98cbb27ed",
        "bb2af328bc77965a1e10de0f4cd34e44",
        "9971ec7c75dd2077e5f21aa4cdc251d5",
        "f22bcbc8b8745b54876417d1470d00e4",
        "2d73dbc4cc54f6cd79982fdca2b823f6",
        "53225bbfe27d27a31c1bde7fe71e2cb3",
        "784bb627a35952f9e5020edc1e00471b",
        "aacc2b9ad4d890ad26b789bc2f739fa0",
        "a78a5516371ac4da7fb3848782cfe81f",
        "3914071c1071f33fb027a1e7703a0280",
        "c29603d1704aaa355a1e6b5d256cbd02",
        "784bd4624dd54557d986a4a75d45b92a",
        "ffc97e9dbd0a113609158bd5517ca264",
        "ad8c9ff9968eb132b5e88c5fe8565043",
        "1c435df244febe91804d5973df562393",
        "9724026dedff5f66770e163f0638e7a6",
        "eb18c8cea1bae69227bb95bfa582cacc",
        "ef7bf43a33ade1084b34f2dc665bd70c",
        "681ce8fa919d941141523bec93cb25c3",
        "b1bc0b81845a715e2952d72bef2cfea4",
        "e1d749b6102553af2f80d3b0aa7eeecf",
        "84aaabaab2614649e3ccdeb0b2f6be80",
        "901535867a405f4815743b7a47cc42a2",
        "2cb74b2542d0d429d02f7aa749aa3bb1",
        "86f4fba7085fb19ae871836dbbbe36d1",
        "2a4c70372b09658b62efe45f912e845e",
        "f27a57b6a72eaae5810d6e729845c8c0",
        "487a3823fae34aff152972977980bc11",
        "fa27562bb588a12831788e5a7cd337ac",
        "ac6fb7dc26e39d9a61eb62a36e62fae1",
        "ff58c2e39b80cef09d26f216cff782b4",
        "138165e070d6a2bab34c7974fd69d5e9",
        "19e134f180e9ffc4ce5cb830b7c9819a",
        "5066e2d287240fd21bb276e792395689",
        "c9bbad755bc5012e6acfc8408288a926",
        "b4076dc641c39adf1a52d6672927c622",
        "e486554b26ccd49bfa28136a58523ad9",
        "aea8ac094be73180cabc3d70d027594b",
        "8f52db3274400fe77a547c8f36331a22",
        "b420aedf3bc347b5cfd3c78cecdf466c",
        "6ce3bd274df57e258fa953fd2dae8200",
        "322645e97dead3bef9181a521200b54e",
        "c8c89cd3b7dd1f6a763cf9390fcfaa1f",
        "4ebec7b94163ae9df1fcd26517ebe9f1",
        "c1f1f413627e256289a29c93715e4edd",
        "50be4606222b0bb993d79fa1c689f1b8",
        "73bf564513d3b427280f4ac306212735",
        "c47eb6e8e0feb75b7bf83df13d8f74f7",
        "c1ae66f8e9322ef624dfdaf0c7f3dc53",
        "ba68682ece7e3a5da09d1ec4adcc30c3",
        "af52792b222fe7d2434f40256d160d5c",
        "c0290695efe870e554c1f5ce04d74382",
        "4fe4aced14ca5249a5c3151dc2341ba7",
        "8584834fdfebbf72113bb82ae5329f1e",
        "d4332e52c91ee1bea5ac57f12636728f",
        "12ad3763ba8f47c6f487f8dd99ee0046",
        "f1f743fd41e0fb0d2b0b8d72da42d08f",
        "6782a9bcd59ad8db78649024895323f3",
        "0479194de8820746dad40e9a305b83af"
    )
}
