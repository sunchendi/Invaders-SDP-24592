package screen;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import engine.Cooldown;
import engine.Core;
import engine.GameState;
import engine.Score;
import audio.AudioManager;
import records.Achievement;
import java.util.ArrayList;

/**
 * Implements the score screen.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class ScoreScreen extends Screen {

	/** Milliseconds between changes in user selection. */
	private static final int SELECTION_TIME = 200;
	/** Maximum number of high scores. */
	private static final int MAX_HIGH_SCORE_NUM = 7;
	/** Code of first mayus character. */
	private static final int FIRST_CHAR = 65;
	/** Code of last mayus character. */
	private static final int LAST_CHAR = 90;

	/** Level (stage) that was being played when the game ended. */
	private int level;
	/** Checks if the last stage was failed (no lives left). */
	private boolean stageFailed;
    private final List<Achievement> runUnlocks;
	/** Current score. */
	private int score;
	/** Player lives left. */
	private int livesRemaining;
	/** Total bullets shot by the player. */
	private int bulletsShot;
	/** Total ships destroyed by the player. */
	private int shipsDestroyed;
	/** List of past high scores. */
	private List<Score> highScores;
	/** Checks if current score is a new high score. */
	private boolean isNewRecord;
	/** Player name for record input. */
	private char[] name;
	/** Character of players name selected for change. */
	private int nameCharSelected;
	/** Time between changes in user selection. */
	private Cooldown selectionCooldown;

	/**
	 * Creates a score screen without newly unlocked achievements.
	 * 
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 * @param state
	 *            Completed game state.
	 */
    public ScoreScreen(final int width, final int height, final int fps, final GameState state) {
        this(width,height,fps,state,Collections.<Achievement>emptyList());
    }

    /**
     * Creates a score screen with a snapshot of this run's new achievements.
     *
     * @param width screen width
     * @param height screen height
     * @param fps frames per second
     * @param gameState completed game state
     * @param unlocks achievements unlocked during the completed run
     */
    public ScoreScreen(final int width, final int height, final int fps,
            final GameState gameState, final List<Achievement> unlocks) {
        super(width,height,fps);
        this.runUnlocks = new ArrayList<Achievement>(unlocks);

		this.level = gameState.getLevel();
		this.stageFailed = gameState.getLivesRemaining() == 0;
		this.score = gameState.getScore();
		this.livesRemaining = gameState.getLivesRemaining();
		this.bulletsShot = gameState.getBulletsShot();
		this.shipsDestroyed = gameState.getShipsDestroyed();
		this.isNewRecord = false;
		this.name = "AAA".toCharArray();
		this.nameCharSelected = 0;
		this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
		this.selectionCooldown.reset();

		try {
			this.highScores = Core.getFileManager().loadHighScores();
			if (highScores.size() < MAX_HIGH_SCORE_NUM
					|| highScores.get(highScores.size() - 1).getScore()
					< this.score)
				this.isNewRecord = true;

		} catch (IOException e) {
			logger.warning("Couldn't load high scores!");
		}


	}

	/**
	 * Starts the action.
	 * 
	 * @return Next screen code.
	 */
	public final int run() {
		super.run();

		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected final void update() {
		super.update();

		draw();
		if (this.inputDelay.checkFinished()) {
			if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
				// Return to main menu.
				AudioManager.playSFX("audio/sfx/menu_back.wav");
				this.returnCode = 1;
				this.isRunning = false;
				if (this.isNewRecord)
					saveScore();
			} else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
				// Play again.
				AudioManager.playSFX("audio/sfx/menu_select.wav");
				this.returnCode = 2;
				this.isRunning = false;
				if (this.isNewRecord)
					saveScore();
			}

			if (this.isNewRecord && this.selectionCooldown.checkFinished()) {
				if (inputManager.isKeyDown(KeyEvent.VK_RIGHT)) {
					this.nameCharSelected = this.nameCharSelected == 2 ? 0
							: this.nameCharSelected + 1;
					this.selectionCooldown.reset();
					AudioManager.playSFX("audio/sfx/name_cursor_move.wav");
				}
				if (inputManager.isKeyDown(KeyEvent.VK_LEFT)) {
					this.nameCharSelected = this.nameCharSelected == 0 ? 2
							: this.nameCharSelected - 1;
					this.selectionCooldown.reset();
					AudioManager.playSFX("audio/sfx/name_cursor_move.wav");
				}
				if (inputManager.isKeyDown(KeyEvent.VK_UP)) {
					this.name[this.nameCharSelected] =
							(char) (this.name[this.nameCharSelected]
									== LAST_CHAR ? FIRST_CHAR
							: this.name[this.nameCharSelected] + 1);
					this.selectionCooldown.reset();
					AudioManager.playSFX("audio/sfx/name_char_change.wav");
				}
				if (inputManager.isKeyDown(KeyEvent.VK_DOWN)) {
					this.name[this.nameCharSelected] =
							(char) (this.name[this.nameCharSelected]
									== FIRST_CHAR ? LAST_CHAR
							: this.name[this.nameCharSelected] - 1);
					this.selectionCooldown.reset();
					AudioManager.playSFX("audio/sfx/name_char_change.wav");
				}
			}
		}

	}

	/**
	 * Saves the score as a high score.
	 */
	private void saveScore() {
		highScores.add(new Score(new String(this.name), score));
		Collections.sort(highScores);
		if (highScores.size() > MAX_HIGH_SCORE_NUM)
			highScores.remove(highScores.size() - 1);

		try {
			Core.getFileManager().saveHighScores(highScores);
		} catch (IOException e) {
			logger.warning("Couldn't load high scores!");
		}
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);
        float accuracy = this.bulletsShot == 0 ? 0 : (float)this.shipsDestroyed / this.bulletsShot;
        if (!this.runUnlocks.isEmpty()) {
            drawManager.drawAchievementResults(this,this.score,this.livesRemaining,
                    this.shipsDestroyed,accuracy,this.isNewRecord,this.name,this.nameCharSelected,
                    this.runUnlocks,this.inputDelay.checkFinished(), this.stageFailed ? this.level : 0);
            drawManager.completeDrawing(this); return;
        }

		drawManager.drawGameOver(this, this.inputDelay.checkFinished(),
				this.isNewRecord);
		if (this.stageFailed)
			drawManager.drawStageFailed(this, this.level, this.isNewRecord);
		drawManager.drawResults(this, this.score, this.livesRemaining,
				this.shipsDestroyed, accuracy, this.isNewRecord);

		if (this.isNewRecord)
			drawManager.drawNameInput(this, this.name, this.nameCharSelected);

		drawManager.completeDrawing(this);
	}

}
