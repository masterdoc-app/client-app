package pro.masterdoc.client.platform

import pro.masterdoc.client.auth.IsoDates

actual fun localEpochDay(): Long =
    checkNotNull(IsoDates.parseToEpochDay(localIsoDate())) {
        "Browser returned an invalid local calendar date"
    }

@JsFun(
    """
    () => {
      const date = new Date();
      const year = date.getFullYear().toString().padStart(4, '0');
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      return `${'$'}{year}-${'$'}{month}-${'$'}{day}`;
    }
    """,
)
private external fun localIsoDate(): String
