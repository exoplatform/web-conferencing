<template>
  <v-row justify="center">
    <v-btn color="primary" dark @click.stop="dialog = true">Open Dialog</v-btn>
    <v-dialog v-model="dialog" width="390">
      <v-card>
        <!-- <v-card-title class="headline">UserName</v-card-title> -->
        <v-avatar color="indigo">
          <v-icon dark>mdi-account-circle</v-icon>
        </v-avatar>
        <v-card-text>UserName started a Meeting with you</v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>

          <v-btn color="success" fab dark @click="dialog = false">
            <i class="uiIconSocPhone"></i>
          </v-btn>Join
          <v-spacer></v-spacer>
          <!-- <v-btn color="green-darken-1" text @click="dialog = false">Disagree</v-btn> -->
          <v-btn class="ma-2" outlined fab color="teal" @click="dialog = false">
            <i class="uiIconClose"></i>
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-row>
</template>
<script>
export default {
  name: "NotificationPopUp",
  data() {
    return {
      dialog: false
    };
  },
  created() {
    console.log(webConferencing.onUserUpdate("root"), "webconf")
  }
};
</script>
<style scoped>
    [class^="uiIcon"] {
        content: "e937"
    }
</style>