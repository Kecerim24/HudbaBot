package com.jagrosh.jmusicbot.utils;

import com.jagrosh.jmusicbot.entities.Pair;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistsItemsRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SpotifyUtil {

    private final String clientId = "7f6ba766ff4f40f0ab9626cf7b99ba87";
    private final String clientSecret = "01f934e0d35e41d3a608044d473dbd1b";

    private final SpotifyApi spotifyApiClient = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .build();

    private final ClientCredentialsRequest clientCredentialsRequest = spotifyApiClient.clientCredentials()
            .build();

    public SpotifyApi getAccessToken() {

        try {
            CompletableFuture<ClientCredentials> clientCredentialsFuture = clientCredentialsRequest.executeAsync();

            final ClientCredentials clientCredentials = clientCredentialsFuture.join();

            spotifyApiClient.setAccessToken(clientCredentials.getAccessToken());

            return new SpotifyApi.Builder().setAccessToken(clientCredentials.getAccessToken()).build();
        } catch (CompletionException e) {
            System.out.println("Error: " + e.getCause().getMessage());
        } catch (CancellationException e) {
            System.out.println("Async operation cancelled.");
        }
        return null;
    }

    public Track getTrack(String trackId) {

        final GetTrackRequest getTrackRequest = getAccessToken()
                .getTrack(trackId)
                .build();

        try {
            final CompletableFuture<Track> trackFuture = getTrackRequest.executeAsync();

            return trackFuture.join();
        } catch (CompletionException e) {
            System.out.println("Error: " + e.getCause().getMessage());
        } catch (CancellationException e) {
            System.out.println("Async operation cancelled.");
        }
        return null;
    }

    public ArrayList<Pair<String, String>> getPlaylist(String playlistId) {

        GetPlaylistsItemsRequest getPlaylistsItemsRequest = getAccessToken()
                .getPlaylistsItems(playlistId)
                .build();
        final CompletableFuture<Paging<PlaylistTrack>> pagingFuture = getPlaylistsItemsRequest.executeAsync();

        ArrayList<Pair<String, String>> nameList1 = new ArrayList<>();

        Paging<PlaylistTrack> playlistTrackPaging = pagingFuture.join();
        PlaylistTrack[] items = playlistTrackPaging.getItems();


        for(PlaylistTrack item: items){
            try {
                Pair<String, String> pair = new Pair<>(getTrack(item.getTrack().getId()).getArtists()[0].getName(),item.getTrack().getName());
                nameList1.add(pair);

            } catch (NullPointerException e){
                System.out.println("Not Found (Spotify)");
            }
        }

        return nameList1;

        /*} catch (CompletionException e) {
            System.out.println("Error: " + e.getCause().getMessage());
        } catch (CancellationException e) {
            System.out.println("Async operation cancelled.");
        }*/
        //return new ArrayList<>();
    }
}

